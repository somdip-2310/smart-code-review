package com.somdiproy.smartcode.service;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.dynamodbv2.util.TableUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.somdiproy.smartcode.dto.CodeReviewResult;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class DynamoDBAnalysisStorage {
    private static final Logger logger = LoggerFactory.getLogger(DynamoDBAnalysisStorage.class);
    
    @Value("${aws.dynamodb.table-name:code-analysis-results}")
    private String tableName;
    
    @Value("${aws.region:us-east-1}")
    private String awsRegion;
    
    private final AmazonDynamoDB dynamoDB;
    private DynamoDBMapper mapper;
    private ObjectMapper objectMapper;
    
    // Constructor injection for AmazonDynamoDB
    public DynamoDBAnalysisStorage(AmazonDynamoDB dynamoDB) {
        this.dynamoDB = dynamoDB;
        this.objectMapper = new ObjectMapper();
    }
    
    @PostConstruct
    public void init() {
        this.mapper = new DynamoDBMapper(dynamoDB);
        
        // Create table if it doesn't exist
        createTableIfNotExists();
        
        logger.info("DynamoDB storage initialized with table: {}", tableName);
    }
    
    @DynamoDBTable(tableName = "code-analysis-results")
    public static class AnalysisRecord {
        private String analysisId;
        private String status;
        private String message;
        private String resultJson;
        private Long timestamp;
        private Long ttl;
        
        @DynamoDBHashKey(attributeName = "analysisId")
        public String getAnalysisId() { return analysisId; }
        public void setAnalysisId(String analysisId) { this.analysisId = analysisId; }
        
        @DynamoDBAttribute(attributeName = "status")
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        @DynamoDBAttribute(attributeName = "message")
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        @DynamoDBAttribute(attributeName = "result")
        public String getResultJson() { return resultJson; }
        public void setResultJson(String resultJson) { this.resultJson = resultJson; }
        
        @DynamoDBAttribute(attributeName = "timestamp")
        public Long getTimestamp() { return timestamp; }
        public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
        
        @DynamoDBAttribute(attributeName = "ttl")
        public Long getTtl() { return ttl; }
        public void setTtl(Long ttl) { this.ttl = ttl; }
        
        // Transient field for easier access
        @DynamoDBIgnore
        private CodeReviewResult result;
        
        @DynamoDBIgnore
        public CodeReviewResult getResult() { return result; }
        public void setResult(CodeReviewResult result) { this.result = result; }
    }
    
    private void createTableIfNotExists() {
        try {
            dynamoDB.describeTable(tableName);
            logger.info("DynamoDB table {} already exists", tableName);
        } catch (ResourceNotFoundException e) {
            logger.info("Creating DynamoDB table: {}", tableName);
            
            // Create table with on-demand billing
            CreateTableRequest createTableRequest = new CreateTableRequest()
                .withTableName(tableName)
                .withKeySchema(new KeySchemaElement("analysisId", KeyType.HASH))
                .withAttributeDefinitions(new AttributeDefinition("analysisId", ScalarAttributeType.S))
                .withBillingMode(BillingMode.PAY_PER_REQUEST); // On-demand billing
            
            dynamoDB.createTable(createTableRequest);
            
            try {
                // Wait for table to be created
                TableUtils.waitUntilActive(dynamoDB, tableName);
                logger.info("DynamoDB table {} created successfully", tableName);
                
                // Configure TTL
                UpdateTimeToLiveRequest ttlRequest = new UpdateTimeToLiveRequest()
                    .withTableName(tableName)
                    .withTimeToLiveSpecification(new TimeToLiveSpecification()
                        .withAttributeName("ttl")
                        .withEnabled(true));
                
                dynamoDB.updateTimeToLive(ttlRequest);
                logger.info("TTL enabled on table {}", tableName);
                
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for table creation", ie);
            }
        }
    }
    
    public void saveAnalysisStatus(String analysisId, String status, String message) {
        try {
            AnalysisRecord record = new AnalysisRecord();
            record.setAnalysisId(analysisId);
            record.setStatus(status);
            record.setMessage(message);
            record.setTimestamp(System.currentTimeMillis());
            record.setTtl(System.currentTimeMillis() / 1000 + 604800); // 7 days TTL
            
            mapper.save(record);
            logger.debug("Saved analysis status: {} - {}", analysisId, status);
            
        } catch (Exception e) {
            logger.error("Error saving analysis status", e);
            throw new RuntimeException("Failed to save analysis status", e);
        }
    }
    
    public void saveAnalysisStatusWithMetadata(String analysisId, String status, String message, Map<String, Object> metadata) {
        try {
            AnalysisRecord record = new AnalysisRecord();
            record.setAnalysisId(analysisId);
            record.setStatus(status);
            record.setMessage(message);
            record.setTimestamp(System.currentTimeMillis());
            record.setTtl(System.currentTimeMillis() / 1000 + 604800); // 7 days TTL
            
            if (metadata != null) {
                record.setResultJson(objectMapper.writeValueAsString(metadata));
            }
            
            mapper.save(record);
            logger.debug("Saved analysis status with metadata: {} - {}", analysisId, status);
            
        } catch (Exception e) {
            logger.error("Error saving analysis status", e);
            throw new RuntimeException("Failed to save analysis status", e);
        }
    }
    
    public void saveAnalysisResult(String analysisId, String status, String message, CodeReviewResult result) {
        try {
            AnalysisRecord record = new AnalysisRecord();
            record.setAnalysisId(analysisId);
            record.setStatus(status);
            record.setMessage(message);
            record.setTimestamp(System.currentTimeMillis());
            record.setTtl(System.currentTimeMillis() / 1000 + 604800); // 7 days TTL
            
            if (result != null) {
                record.setResultJson(objectMapper.writeValueAsString(result));
            }
            
            mapper.save(record);
            logger.info("Saved analysis result: {} - {}", analysisId, status);
            
        } catch (Exception e) {
            logger.error("Error saving analysis result", e);
            throw new RuntimeException("Failed to save analysis result", e);
        }
    }
    
    public AnalysisRecord getAnalysis(String analysisId) {
        try {
            AnalysisRecord record = mapper.load(AnalysisRecord.class, analysisId);
            
            if (record != null && record.getResultJson() != null) {
                // Parse the JSON result
                CodeReviewResult result = objectMapper.readValue(record.getResultJson(), CodeReviewResult.class);
                record.setResult(result);
            }
            
            return record;
            
        } catch (Exception e) {
            logger.error("Error retrieving analysis", e);
            throw new RuntimeException("Failed to retrieve analysis", e);
        }
    }
    
    public boolean deleteAnalysis(String analysisId) {
        try {
            AnalysisRecord record = new AnalysisRecord();
            record.setAnalysisId(analysisId);
            mapper.delete(record);
            return true;
            
        } catch (Exception e) {
            logger.error("Error deleting analysis", e);
            return false;
        }
    }
}