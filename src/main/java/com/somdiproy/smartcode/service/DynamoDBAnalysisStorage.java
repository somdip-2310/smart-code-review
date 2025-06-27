package com.somdiproy.smartcode.service;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.dynamodbv2.util.TableUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.somdiproy.smartcode.dto.CodeReviewResult;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import java.util.HashMap;
import java.util.ArrayList;

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
        
        @DynamoDBAttribute(attributeName = "resultJson")  // Changed from "result" to "resultJson"
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
            // First, try to load using the standard mapper
            AnalysisRecord record = null;
            
            try {
                record = mapper.load(AnalysisRecord.class, analysisId);
            } catch (DynamoDBMappingException e) {
                // If standard loading fails, try manual loading
                logger.warn("Standard loading failed, attempting manual load for: {}", analysisId);
                record = manualLoadRecord(analysisId);
            }
            
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

    private AnalysisRecord manualLoadRecord(String analysisId) {
        try {
            // Use low-level DynamoDB API to get the item
            GetItemRequest getItemRequest = new GetItemRequest()
                .withTableName(tableName)
                .withKey(Map.of("analysisId", new AttributeValue(analysisId)));
                
            GetItemResult result = dynamoDB.getItem(getItemRequest);
            
            if (result.getItem() == null) {
                return null;
            }
            
            Map<String, AttributeValue> item = result.getItem();
            AnalysisRecord record = new AnalysisRecord();
            
            // Map basic fields
            record.setAnalysisId(analysisId);
            record.setStatus(item.getOrDefault("status", new AttributeValue()).getS());
            record.setMessage(item.getOrDefault("message", new AttributeValue()).getS());
            
            if (item.containsKey("timestamp") && item.get("timestamp").getN() != null) {
                record.setTimestamp(Long.parseLong(item.get("timestamp").getN()));
            }
            
            if (item.containsKey("ttl") && item.get("ttl").getN() != null) {
                record.setTtl(Long.parseLong(item.get("ttl").getN()));
            }
            
            // Handle the result field - it might be stored as a Map or as a JSON string
            if (item.containsKey("result")) {
                AttributeValue resultAttr = item.get("result");
                
                if (resultAttr.getS() != null) {
                    // It's already a JSON string
                    record.setResultJson(resultAttr.getS());
                } else if (resultAttr.getM() != null) {
                    // It's a Map - convert to JSON
                    Map<String, Object> resultMap = convertAttributeValueMapToObject(resultAttr.getM());
                    record.setResultJson(objectMapper.writeValueAsString(resultMap));
                }
            } else if (item.containsKey("resultJson")) {
                // Check for resultJson field as well
                AttributeValue resultJsonAttr = item.get("resultJson");
                if (resultJsonAttr != null && resultJsonAttr.getS() != null) {
                    record.setResultJson(resultJsonAttr.getS());
                }
            }
            
            return record;
            
        } catch (Exception e) {
            logger.error("Error in manual load", e);
            return null;
        }
    }

    private Map<String, Object> convertAttributeValueMapToObject(Map<String, AttributeValue> attributeMap) {
        Map<String, Object> result = new HashMap<>();
        
        for (Map.Entry<String, AttributeValue> entry : attributeMap.entrySet()) {
            String key = entry.getKey();
            AttributeValue value = entry.getValue();
            
            if (value.getS() != null) {
                result.put(key, value.getS());
            } else if (value.getN() != null) {
                result.put(key, Double.parseDouble(value.getN()));
            } else if (value.getBOOL() != null) {
                result.put(key, value.getBOOL());
            } else if (value.getL() != null) {
                result.put(key, convertAttributeValueListToObject(value.getL()));
            } else if (value.getM() != null) {
                result.put(key, convertAttributeValueMapToObject(value.getM()));
            }
        }
        
        return result;
    }

    private List<Object> convertAttributeValueListToObject(List<AttributeValue> attributeList) {
        List<Object> result = new ArrayList<>();
        
        for (AttributeValue value : attributeList) {
            if (value.getS() != null) {
                result.add(value.getS());
            } else if (value.getN() != null) {
                result.add(Double.parseDouble(value.getN()));
            } else if (value.getBOOL() != null) {
                result.add(value.getBOOL());
            } else if (value.getL() != null) {
                result.add(convertAttributeValueListToObject(value.getL()));
            } else if (value.getM() != null) {
                result.add(convertAttributeValueMapToObject(value.getM()));
            }
        }
        
        return result;
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