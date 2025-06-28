package com.somdiproy.smartcode.service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

@Service
public class SQSBedrockService {
    private static final Logger logger = LoggerFactory.getLogger(SQSBedrockService.class);
    
    @Value("${aws.sqs.queue-name:bedrock-analysis-queue}")
    private String queueName;
    
    @Value("${aws.sqs.visibility-timeout:960}")
    private Integer visibilityTimeout;
    
    @Value("${aws.region:us-east-1}")
    private String awsRegion;
    
    // Staggered delay configuration
    @Value("${analysis.queue.base.delay.seconds:30}")
    private int baseDelaySeconds;
    
    @Value("${analysis.queue.delay.per.message.seconds:10}")
    private int delayPerMessageSeconds;
    
    @Value("${analysis.queue.max.delay.seconds:300}")
    private int maxDelaySeconds;
    
    // Rate limiting configuration
    @Value("${bedrock.rate.limit.per.minute:3}")
    private int bedrockRateLimitPerMinute;
    
    private AmazonSQS sqs;
    private ObjectMapper objectMapper;
    private String queueUrl;
    private final S3Service s3Service;

    public SQSBedrockService(AmazonSQS sqs, S3Service s3Service) {
        this.sqs = sqs;
        this.s3Service = s3Service;
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void init() {
        try {
            // Get queue URL or create if doesn't exist
            try {
                GetQueueUrlResult getQueueUrlResult = sqs.getQueueUrl(queueName);
                this.queueUrl = getQueueUrlResult.getQueueUrl();
                logger.info("Using existing SQS queue: {}", queueUrl);
                
                // Update queue attributes to ensure proper configuration
                updateQueueAttributes();
                
            } catch (QueueDoesNotExistException e) {
                // Create queue if it doesn't exist
                CreateQueueRequest createQueueRequest = new CreateQueueRequest(queueName)
                    .addAttributesEntry("VisibilityTimeout", visibilityTimeout.toString())
                    .addAttributesEntry("MessageRetentionPeriod", "345600") // 4 days
                    .addAttributesEntry("ReceiveMessageWaitTimeSeconds", "20") // Long polling
                    .addAttributesEntry("DelaySeconds", String.valueOf(baseDelaySeconds)); // Base delay
                    
                CreateQueueResult createResult = sqs.createQueue(createQueueRequest);
                this.queueUrl = createResult.getQueueUrl();
                logger.info("Created new SQS queue: {}", queueUrl);
            }
            
        } catch (Exception e) {
            logger.error("Failed to initialize SQS service", e);
            throw new RuntimeException("Failed to initialize SQS service", e);
        }
    }
    
    private void updateQueueAttributes() {
        try {
            SetQueueAttributesRequest request = new SetQueueAttributesRequest()
                .withQueueUrl(queueUrl)
                .addAttributesEntry("DelaySeconds", String.valueOf(baseDelaySeconds))
                .addAttributesEntry("VisibilityTimeout", visibilityTimeout.toString());
            
            sqs.setQueueAttributes(request);
            logger.info("Updated queue attributes - Base delay: {}s, Visibility timeout: {}s", 
                baseDelaySeconds, visibilityTimeout);
        } catch (Exception e) {
            logger.warn("Failed to update queue attributes: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void cleanup() {
        logger.info("SQS service cleanup completed");
    }
    
    /**
     * Calculate dynamic delay based on queue depth and rate limits
     */
    private int calculateMessageDelay() {
        try {
            // Get current queue metrics
            GetQueueAttributesRequest getAttrsRequest = new GetQueueAttributesRequest()
                .withQueueUrl(queueUrl)
                .withAttributeNames("ApproximateNumberOfMessages", 
                                  "ApproximateNumberOfMessagesNotVisible",
                                  "ApproximateNumberOfMessagesDelayed");
            
            GetQueueAttributesResult attrsResult = sqs.getQueueAttributes(getAttrsRequest);
            Map<String, String> attrs = attrsResult.getAttributes();
            
            int visibleMessages = Integer.parseInt(attrs.getOrDefault("ApproximateNumberOfMessages", "0"));
            int invisibleMessages = Integer.parseInt(attrs.getOrDefault("ApproximateNumberOfMessagesNotVisible", "0"));
            int delayedMessages = Integer.parseInt(attrs.getOrDefault("ApproximateNumberOfMessagesDelayed", "0"));
            
            int totalMessages = visibleMessages + invisibleMessages + delayedMessages;
            
            // Calculate dynamic delay
            // Base delay + (number of messages * delay per message)
            int calculatedDelay = baseDelaySeconds + (totalMessages * delayPerMessageSeconds);
            
            // Ensure delay doesn't exceed maximum
            int finalDelay = Math.min(calculatedDelay, maxDelaySeconds);
            
            // Additional delay if we're approaching Bedrock rate limit
            // Spread messages to stay under rate limit (e.g., 3 per minute = 1 every 20 seconds minimum)
            int minIntervalSeconds = 60 / bedrockRateLimitPerMinute;
            if (invisibleMessages > 0 && finalDelay < minIntervalSeconds) {
                finalDelay = minIntervalSeconds;
            }
            
            logger.info("Queue depth - Visible: {}, In-flight: {}, Delayed: {}, Total: {} => Delay: {}s",
                visibleMessages, invisibleMessages, delayedMessages, totalMessages, finalDelay);
            
            return finalDelay;
            
        } catch (Exception e) {
            logger.error("Error calculating message delay, using base delay", e);
            return baseDelaySeconds;
        }
    }
   
    public String submitAnalysisRequest(String analysisId, String code, String language) {
        try {
            // Calculate dynamic delay based on current queue state
            int messageDelay = calculateMessageDelay();
            
            Map<String, Object> message = new HashMap<>();
            message.put("analysisId", analysisId);
            message.put("codeLength", code.length());
            message.put("language", language);
            message.put("timestamp", System.currentTimeMillis());
            message.put("scheduledProcessingTime", System.currentTimeMillis() + (messageDelay * 1000L));
            
            // Calculate message size (rough estimate including JSON overhead)
            int estimatedMessageSize = code.length() + 1000; // 1KB for metadata
            
            // Check if message would exceed SQS limit (256KB = 262144 bytes)
            // Use 250KB as safe threshold to account for encoding overhead
            if (estimatedMessageSize > 250000) {
                logger.info("Code too large for SQS ({} chars), storing in S3 first", code.length());
                
                // Generate consistent S3 key
                String s3Key = String.format("analysis/%s/code.txt", analysisId);
                
                // Store code in S3 - DO NOT fallback to inline for large files
                try {
                    s3Service.uploadCodeContent(code, s3Key, analysisId);
                    message.put("codeLocation", "s3");
                    message.put("s3Key", s3Key);
                    // Don't include the code in the message when using S3
                    logger.info("Code stored in S3 with key: {}", s3Key);
                } catch (Exception e) {
                    logger.error("Failed to upload code to S3 for analysis {}: {}", analysisId, e.getMessage(), e);
                    // For large files, we cannot fallback to inline - it will fail
                    throw new RuntimeException("Code too large for inline processing and S3 upload failed", e);
                }
            } else {
                // Small enough to send inline
                message.put("code", code);
                message.put("codeLocation", "inline");
                logger.info("Sending code inline ({} chars)", code.length());
            }
            
            String messageBody = objectMapper.writeValueAsString(message);
            
            // Log actual message size for debugging
            logger.info("SQS message size: {} bytes for analysis {}", messageBody.getBytes().length, analysisId);
            
            // Build send request with dynamic delay
            SendMessageRequest sendRequest = new SendMessageRequest()
                .withQueueUrl(queueUrl)
                .withMessageBody(messageBody)
                .withDelaySeconds(messageDelay)  // Use calculated delay
                .withMessageAttributes(Map.of(
                    "analysisId", new MessageAttributeValue()
                        .withStringValue(analysisId)
                        .withDataType("String"),
                    "language", new MessageAttributeValue()
                        .withStringValue(language)
                        .withDataType("String"),
                    "retryCount", new MessageAttributeValue()
                        .withStringValue("0")
                        .withDataType("Number"),
                    "submittedAt", new MessageAttributeValue()
                        .withStringValue(String.valueOf(System.currentTimeMillis()))
                        .withDataType("Number")
                ));
                
            SendMessageResult result = sqs.sendMessage(sendRequest);
            logger.info("Submitted analysis request {} to SQS: {} with delay of {} seconds", 
                analysisId, result.getMessageId(), messageDelay);
            
            return result.getMessageId();
            
        } catch (Exception e) {
            logger.error("Error submitting to SQS", e);
            throw new RuntimeException("Failed to submit analysis request", e);
        }
    }
    
    /**
     * Get current queue depth and metrics
     */
    public Map<String, Object> getQueueMetrics() {
        try {
            GetQueueAttributesRequest request = new GetQueueAttributesRequest()
                .withQueueUrl(queueUrl)
                .withAttributeNames("All");
                
            GetQueueAttributesResult result = sqs.getQueueAttributes(request);
            Map<String, String> attrs = result.getAttributes();
            
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("visibleMessages", Integer.parseInt(attrs.getOrDefault("ApproximateNumberOfMessages", "0")));
            metrics.put("inFlightMessages", Integer.parseInt(attrs.getOrDefault("ApproximateNumberOfMessagesNotVisible", "0")));
            metrics.put("delayedMessages", Integer.parseInt(attrs.getOrDefault("ApproximateNumberOfMessagesDelayed", "0")));
            metrics.put("queueCreatedTimestamp", attrs.get("CreatedTimestamp"));
            metrics.put("lastModifiedTimestamp", attrs.get("LastModifiedTimestamp"));
            metrics.put("visibilityTimeout", attrs.get("VisibilityTimeout"));
            metrics.put("messageRetentionPeriod", attrs.get("MessageRetentionPeriod"));
            metrics.put("delaySeconds", attrs.get("DelaySeconds"));
            
            // Calculate next available processing slot
            int totalActive = (int) metrics.get("inFlightMessages") + (int) metrics.get("delayedMessages");
            int nextDelay = calculateMessageDelay();
            metrics.put("nextAvailableSlot", System.currentTimeMillis() + (nextDelay * 1000L));
            metrics.put("calculatedDelay", nextDelay);
            
            return metrics;
            
        } catch (Exception e) {
            logger.error("Error getting queue metrics", e);
            return Map.of("error", e.getMessage());
        }
    }
    
    /**
     * Get queue depth (backward compatibility)
     */
    public int getQueueDepth() {
        Map<String, Object> metrics = getQueueMetrics();
        return (int) metrics.getOrDefault("visibleMessages", -1) + 
               (int) metrics.getOrDefault("inFlightMessages", 0) +
               (int) metrics.getOrDefault("delayedMessages", 0);
    }
    
    /**
     * Check if queue is at capacity based on rate limits
     */
    public boolean isQueueAtCapacity() {
        int queueDepth = getQueueDepth();
        // Consider queue at capacity if we have more than 5 minutes worth of messages
        int maxQueueDepth = bedrockRateLimitPerMinute * 5;
        return queueDepth >= maxQueueDepth;
    }
}