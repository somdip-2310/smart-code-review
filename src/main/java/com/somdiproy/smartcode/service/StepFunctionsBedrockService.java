package com.somdiproy.smartcode.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sfn.model.StartExecutionRequest;
import software.amazon.awssdk.services.sfn.model.StartExecutionResponse;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Service
public class StepFunctionsBedrockService {
    private static final Logger logger = LoggerFactory.getLogger(StepFunctionsBedrockService.class);
    
    @Value("${aws.stepfunctions.state-machine-arn:}")
    private String stateMachineArn;
    
    @Value("${aws.stepfunctions.enabled:false}")
    private boolean enabled;
    
    private SfnClient sfnClient;
    private ObjectMapper objectMapper;
    
    @PostConstruct
    public void init() {
        if (enabled && !stateMachineArn.isEmpty()) {
            this.sfnClient = SfnClient.builder().build();
            this.objectMapper = new ObjectMapper();
            logger.info("Step Functions service initialized with state machine: {}", stateMachineArn);
        } else {
            logger.warn("Step Functions service is disabled or not configured");
        }
    }
    
    public String submitAnalysisToStepFunctions(String analysisId, String code, String language) {
        if (!enabled) {
            throw new IllegalStateException("Step Functions service is not enabled");
        }
        
        try {
            Map<String, Object> input = new HashMap<>();
            input.put("analysisId", analysisId);
            input.put("code", code);
            input.put("language", language);
            input.put("timestamp", System.currentTimeMillis());
            
            StartExecutionRequest request = StartExecutionRequest.builder()
                .stateMachineArn(stateMachineArn)
                .name("analysis-" + analysisId)
                .input(objectMapper.writeValueAsString(input))
                .build();
                
            StartExecutionResponse response = sfnClient.startExecution(request);
            logger.info("Started Step Functions execution: {}", response.executionArn());
            
            return response.executionArn();
            
        } catch (Exception e) {
            logger.error("Failed to start Step Functions execution", e);
            throw new RuntimeException("Failed to submit analysis to Step Functions", e);
        }
    }
    
    public boolean isEnabled() {
        return enabled;
    }
}