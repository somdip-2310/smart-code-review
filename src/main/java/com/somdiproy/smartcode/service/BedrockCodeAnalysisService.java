package com.somdiproy.smartcode.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.somdiproy.smartcode.model.CodeAnalysisResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import javax.annotation.PostConstruct;
import java.util.*;

@Service
public class BedrockCodeAnalysisService {
    
    private static final Logger logger = LoggerFactory.getLogger(BedrockCodeAnalysisService.class);
    
    private final BedrockRuntimeClient bedrockRuntimeClient;
    private final ObjectMapper objectMapper;
    
    @Value("${aws.bedrock.model-id:us.amazon.nova-premier-v1:0}")
    private String modelId;
    
    public BedrockCodeAnalysisService(BedrockRuntimeClient bedrockRuntimeClient) {
        this.bedrockRuntimeClient = bedrockRuntimeClient;
        this.objectMapper = new ObjectMapper();
    }
    
    @PostConstruct
    public void init() {
        logger.info("BedrockCodeAnalysisService initialized with model: {}", modelId);
    }
    
    public CodeAnalysisResult analyzeCode(String sessionId, String codeContent, List<String> fileNames) {
        long startTime = System.currentTimeMillis();
        
        try {
            logger.info("Starting code analysis for session: {}", sessionId);
            
            String analysisPrompt = buildAnalysisPrompt(codeContent, fileNames);
            String aiResponse = invokeBedrockModel(analysisPrompt);
            
            CodeAnalysisResult result = parseAnalysisResponse(aiResponse);
            result.setAnalysisId(UUID.randomUUID().toString());
            result.setSessionId(sessionId);
            result.setStatus(CodeAnalysisResult.AnalysisStatus.COMPLETED);
            result.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            
            logger.info("Code analysis completed for session: {} in {}ms", sessionId, result.getProcessingTimeMs());
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error analyzing code for session: {}", sessionId, e);
            
            CodeAnalysisResult errorResult = new CodeAnalysisResult();
            errorResult.setAnalysisId(UUID.randomUUID().toString());
            errorResult.setSessionId(sessionId);
            errorResult.setStatus(CodeAnalysisResult.AnalysisStatus.FAILED);
            errorResult.setSummary("Analysis failed due to technical error: " + e.getMessage());
            errorResult.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            
            return errorResult;
        }
    }
    
    private String buildAnalysisPrompt(String codeContent, List<String> fileNames) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are an expert code reviewer and security analyst. ");
        prompt.append("Please analyze the following code and provide a comprehensive review.\n\n");
        
        prompt.append("Files to analyze:\n");
        if (fileNames != null && !fileNames.isEmpty()) {
            for (String fileName : fileNames) {
                prompt.append("- ").append(fileName).append("\n");
            }
        }
        prompt.append("\n");
        
        prompt.append("Code content:\n");
        prompt.append("```\n");
        prompt.append(codeContent);
        prompt.append("\n```\n\n");
        
        prompt.append("Please provide your analysis in the following JSON format:\n");
        prompt.append("{\n");
        prompt.append("  \"overallScore\": \"A score from 1-10\",\n");
        prompt.append("  \"summary\": \"Brief overall assessment\",\n");
        prompt.append("  \"findings\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"type\": \"SECURITY|PERFORMANCE|CODE_QUALITY|BUG\",\n");
        prompt.append("      \"severity\": \"HIGH|MEDIUM|LOW\",\n");
        prompt.append("      \"file\": \"filename if applicable\",\n");
        prompt.append("      \"line\": 0,\n");
        prompt.append("      \"description\": \"What was found\",\n");
        prompt.append("      \"recommendation\": \"How to fix it\"\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"suggestions\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"category\": \"ARCHITECTURE|TESTING|DOCUMENTATION|PERFORMANCE\",\n");
        prompt.append("      \"title\": \"Brief title\",\n");
        prompt.append("      \"description\": \"Detailed suggestion\",\n");
        prompt.append("      \"priority\": \"HIGH|MEDIUM|LOW\"\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"metrics\": {\n");
        prompt.append("    \"linesOfCode\": 0,\n");
        prompt.append("    \"complexity\": \"LOW|MEDIUM|HIGH\",\n");
        prompt.append("    \"maintainability\": \"POOR|FAIR|GOOD|EXCELLENT\",\n");
        prompt.append("    \"testCoverage\": \"UNKNOWN|LOW|MEDIUM|HIGH\"\n");
        prompt.append("  }\n");
        prompt.append("}\n\n");
        
        prompt.append("Focus on:\n");
        prompt.append("1. Security vulnerabilities\n");
        prompt.append("2. Performance issues\n");
        prompt.append("3. Code quality and best practices\n");
        prompt.append("4. Potential bugs\n");
        prompt.append("5. Architecture improvements\n");
        
        return prompt.toString();
    }
    
    private String invokeBedrockModel(String prompt) {
        try {
            String jsonBody = String.format("""
                {
                    "messages": [
                        {
                            "role": "user",
                            "content": [
                                {
                                    "text": "%s"
                                }
                            ]
                        }
                    ],
                    "inferenceConfig": {
                        "maxTokens": 4000,
                        "temperature": 0.3,
                        "topP": 0.9
                    }
                }
                """, escapeJsonString(prompt));
                
            InvokeModelRequest request = InvokeModelRequest.builder()
                    .modelId(modelId)
                    .contentType("application/json")
                    .accept("application/json")
                    .body(SdkBytes.fromUtf8String(jsonBody))
                    .build();
                    
            InvokeModelResponse response = bedrockRuntimeClient.invokeModel(request);
            String responseBody = response.body().asUtf8String();
            
            JsonNode jsonResponse = objectMapper.readTree(responseBody);
            return jsonResponse.path("output").path("message").path("content").get(0).path("text").asText();
            
        } catch (Exception e) {
            logger.error("Error invoking Bedrock model", e);
            throw new RuntimeException("Failed to analyze code with AI", e);
        }
    }
    
    private CodeAnalysisResult parseAnalysisResponse(String aiResponse) {
        try {
            // Extract JSON from the response (it might be wrapped in markdown)
            String jsonResponse = extractJsonFromResponse(aiResponse);
            JsonNode jsonNode = objectMapper.readTree(jsonResponse);
            
            CodeAnalysisResult result = new CodeAnalysisResult();
            
            result.setOverallScore(jsonNode.path("overallScore").asText());
            result.setSummary(jsonNode.path("summary").asText());
            
            // Parse findings
            List<CodeAnalysisResult.Finding> findings = new ArrayList<>();
            JsonNode findingsNode = jsonNode.path("findings");
            if (findingsNode.isArray()) {
                for (JsonNode findingNode : findingsNode) {
                    CodeAnalysisResult.Finding finding = new CodeAnalysisResult.Finding(
                        findingNode.path("type").asText(),
                        findingNode.path("severity").asText(),
                        findingNode.path("file").asText(),
                        findingNode.path("line").asInt(),
                        findingNode.path("description").asText(),
                        findingNode.path("recommendation").asText()
                    );
                    findings.add(finding);
                }
            }
            result.setFindings(findings);
            
            // Parse suggestions
            List<CodeAnalysisResult.Suggestion> suggestions = new ArrayList<>();
            JsonNode suggestionsNode = jsonNode.path("suggestions");
            if (suggestionsNode.isArray()) {
                for (JsonNode suggestionNode : suggestionsNode) {
                    CodeAnalysisResult.Suggestion suggestion = new CodeAnalysisResult.Suggestion(
                        suggestionNode.path("category").asText(),
                        suggestionNode.path("title").asText(),
                        suggestionNode.path("description").asText(),
                        suggestionNode.path("priority").asText()
                    );
                    suggestions.add(suggestion);
                }
            }
            result.setSuggestions(suggestions);
            
            // Parse metrics
            Map<String, Object> metrics = new HashMap<>();
            JsonNode metricsNode = jsonNode.path("metrics");
            metricsNode.fields().forEachRemaining(entry -> {
                metrics.put(entry.getKey(), entry.getValue().asText());
            });
            result.setMetrics(metrics);
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error parsing AI response", e);
            
            // Return a basic result if parsing fails
            CodeAnalysisResult fallbackResult = new CodeAnalysisResult();
            fallbackResult.setOverallScore("N/A");
            fallbackResult.setSummary("Analysis completed but response format was unexpected. Raw response: " + 
                                    (aiResponse.length() > 500 ? aiResponse.substring(0, 500) + "..." : aiResponse));
            fallbackResult.setFindings(new ArrayList<>());
            fallbackResult.setSuggestions(new ArrayList<>());
            fallbackResult.setMetrics(new HashMap<>());
            
            return fallbackResult;
        }
    }
    
    private String extractJsonFromResponse(String response) {
        // Try to find JSON wrapped in code blocks
        int jsonStart = response.indexOf("```json");
        if (jsonStart != -1) {
            jsonStart += 7; // Skip "```json"
            int jsonEnd = response.indexOf("```", jsonStart);
            if (jsonEnd != -1) {
                return response.substring(jsonStart, jsonEnd).trim();
            }
        }
        
        // Try to find JSON wrapped in regular code blocks
        jsonStart = response.indexOf("```");
        if (jsonStart != -1) {
            jsonStart += 3;
            int jsonEnd = response.indexOf("```", jsonStart);
            if (jsonEnd != -1) {
                return response.substring(jsonStart, jsonEnd).trim();
            }
        }
        
        // Try to find JSON by looking for { and }
        jsonStart = response.indexOf("{");
        int jsonEnd = response.lastIndexOf("}");
        if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
            return response.substring(jsonStart, jsonEnd + 1);
        }
        
        // If no JSON found, return the whole response
        return response;
    }
    
    private String escapeJsonString(String str) {
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}