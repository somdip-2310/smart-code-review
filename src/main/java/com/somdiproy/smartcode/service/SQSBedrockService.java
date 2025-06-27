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
            } catch (QueueDoesNotExistException e) {
                // Create queue if it doesn't exist
                CreateQueueRequest createQueueRequest = new CreateQueueRequest(queueName)
                    .addAttributesEntry("VisibilityTimeout", visibilityTimeout.toString())
                    .addAttributesEntry("MessageRetentionPeriod", "86400") // 1 day
                    .addAttributesEntry("ReceiveMessageWaitTimeSeconds", "20") // Long polling
                    .addAttributesEntry("DelaySeconds", "0");
                    
                CreateQueueResult createResult = sqs.createQueue(createQueueRequest);
                this.queueUrl = createResult.getQueueUrl();
                logger.info("Created new SQS queue: {}", queueUrl);
            }
            
        } catch (Exception e) {
            logger.error("Failed to initialize SQS service", e);
            throw new RuntimeException("Failed to initialize SQS service", e);
        }
    }

    @PreDestroy
    public void cleanup() {
        logger.info("SQS service cleanup completed");
    }
    
   
    public String submitAnalysisRequest(String analysisId, String code, String language) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("analysisId", analysisId);
            message.put("codeLength", code.length());
            message.put("language", language);
            message.put("timestamp", System.currentTimeMillis());
            
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
            
            SendMessageRequest sendRequest = new SendMessageRequest()
                .withQueueUrl(queueUrl)
                .withMessageBody(messageBody)
                .withMessageAttributes(Map.of(
                    "analysisId", new MessageAttributeValue()
                        .withStringValue(analysisId)
                        .withDataType("String"),
                    "language", new MessageAttributeValue()
                        .withStringValue(language)
                        .withDataType("String")
                ));
                
            SendMessageResult result = sqs.sendMessage(sendRequest);
            logger.info("Submitted analysis request {} to SQS: {}", analysisId, result.getMessageId());
            
            return result.getMessageId();
            
        } catch (Exception e) {
            logger.error("Error submitting to SQS", e);
            throw new RuntimeException("Failed to submit analysis request", e);
        }
    }
    
    public int getQueueDepth() {
        try {
            GetQueueAttributesRequest request = new GetQueueAttributesRequest()
                .withQueueUrl(queueUrl)
                .withAttributeNames("ApproximateNumberOfMessages");
                
            GetQueueAttributesResult result = sqs.getQueueAttributes(request);
            String count = result.getAttributes().get("ApproximateNumberOfMessages");
            return Integer.parseInt(count);
            
        } catch (Exception e) {
            logger.error("Error getting queue depth", e);
            return -1;
        }
    }
}