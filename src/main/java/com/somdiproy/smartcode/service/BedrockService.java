package com.somdiproy.smartcode.service;

import com.somdiproy.smartcode.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class BedrockService {
    private static final Logger logger = LoggerFactory.getLogger(BedrockService.class);
    
    @Value("${bedrock.processing.mode:async}")
    private String processingMode;
    
    private final SQSBedrockService sqsService;
    private final DynamoDBAnalysisStorage dynamoDBStorage;
    
    public BedrockService(SQSBedrockService sqsService, DynamoDBAnalysisStorage dynamoDBStorage) {
        this.sqsService = sqsService;
        this.dynamoDBStorage = dynamoDBStorage;
    }
    
    /**
     * Submit code for analysis
     */
    public CodeReviewResult analyzeCode(String code, String language) {
        String analysisId = UUID.randomUUID().toString();
        
        try {
            logger.info("Submitting analysis {} for {} code (length: {})", 
                       analysisId, language, code.length());
            
            // Validate input
            if (code == null || code.trim().isEmpty()) {
                return createErrorResult("Code content is empty");
            }
            
            if (language == null || language.trim().isEmpty()) {
                language = "unknown";
            }
            
            // Store initial status in DynamoDB
            dynamoDBStorage.saveAnalysisStatus(analysisId, "QUEUED", "Analysis queued for processing");
            
            // Submit to SQS for async processing
            String messageId = sqsService.submitAnalysisRequest(analysisId, code, language);
            
            logger.info("Analysis {} submitted to queue with message ID: {}", analysisId, messageId);
            
            // Return pending result with analysis ID
            return CodeReviewResult.builder()
                    .summary("Analysis submitted successfully. Processing may take a few minutes.")
                    .overallScore(0.0)
                    .issues(new ArrayList<>())
                    .suggestions(new ArrayList<>())
                    .metadata(Map.of(
                        "analysisId", analysisId,
                        "status", "QUEUED",
                        "message", "Use the analysis ID to check status",
                        "queueMessageId", messageId
                    ))
                    .build();
                    
        } catch (Exception e) {
            logger.error("Error submitting analysis", e);
            dynamoDBStorage.saveAnalysisStatus(analysisId, "FAILED", e.getMessage());
            return createErrorResult("Failed to submit analysis: " + e.getMessage());
        }
    }
    
    /**
     * Submit code for analysis with existing analysis ID
     * This method reuses an existing analysis ID instead of creating a new one
     */
    public void submitAnalysisWithId(String analysisId, String code, String language) {
        try {
            logger.info("Submitting analysis {} for {} code (length: {})", 
                       analysisId, language, code.length());
            
            // Validate input
            if (code == null || code.trim().isEmpty()) {
                dynamoDBStorage.saveAnalysisStatus(analysisId, "FAILED", "Code content is empty");
                return;
            }
            
            if (language == null || language.trim().isEmpty()) {
                language = "unknown";
            }
            
            // Store initial status in DynamoDB  
            dynamoDBStorage.saveAnalysisStatus(analysisId, "QUEUED", "Analysis queued for processing");
            
            // Submit to SQS for async processing
            String messageId = sqsService.submitAnalysisRequest(analysisId, code, language);
            
            logger.info("Analysis {} submitted to queue with message ID: {}", analysisId, messageId);
            
        } catch (Exception e) {
            logger.error("Error submitting analysis", e);
            dynamoDBStorage.saveAnalysisStatus(analysisId, "FAILED", e.getMessage());
        }
    }
    
    /**
     * Get analysis result by ID
     */
    public CodeReviewResult getAnalysisResult(String analysisId) {
        try {
            DynamoDBAnalysisStorage.AnalysisRecord record = dynamoDBStorage.getAnalysis(analysisId);
            
            if (record == null) {
                return createErrorResult("Analysis not found: " + analysisId);
            }
            
            // If still processing, return status
            if (!"COMPLETED".equals(record.getStatus())) {
                return CodeReviewResult.builder()
                        .summary("Analysis in progress")
                        .overallScore(0.0)
                        .metadata(Map.of(
                            "analysisId", analysisId,
                            "status", record.getStatus(),
                            "message", record.getMessage(),
                            "timestamp", record.getTimestamp()
                        ))
                        .build();
            }
            
            // Return completed result
            return record.getResult();
            
        } catch (Exception e) {
            logger.error("Error retrieving analysis result", e);
            return createErrorResult("Failed to retrieve analysis: " + e.getMessage());
        }
    }
    
    /**
     * Get queue status
     */
    public Map<String, Object> getQueueStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("queueDepth", sqsService.getQueueDepth());
        status.put("processingMode", processingMode);
        status.put("timestamp", System.currentTimeMillis());
        return status;
    }
    
    private CodeReviewResult createErrorResult(String errorMessage) {
        return CodeReviewResult.builder()
                .summary("Error: " + errorMessage)
                .overallScore(0.0)
                .issues(new ArrayList<>())
                .suggestions(new ArrayList<>())
                .security(SecurityAnalysis.builder()
                        .securityScore(0.0)
                        .vulnerabilities(new ArrayList<>())
                        .recommendations(new ArrayList<>())
                        .hasSecurityIssues(false)
                        .build())
                .performance(PerformanceAnalysis.builder()
                        .performanceScore(0.0)
                        .bottlenecks(new ArrayList<>())
                        .optimizations(new ArrayList<>())
                        .complexity("Unknown")
                        .build())
                .quality(QualityMetrics.builder()
                        .maintainabilityScore(0.0)
                        .readabilityScore(0.0)
                        .linesOfCode(0)
                        .complexityScore(0)
                        .testCoverage(0.0)
                        .duplicateLines(0)
                        .build())
                .metadata(Map.of("error", true, "message", errorMessage))
                .build();
    }
}