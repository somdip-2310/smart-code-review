// ===== BedrockService.java =====
package com.somdiproy.smartcode.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.somdiproy.smartcode.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BedrockService {
    
    private static final Logger logger = LoggerFactory.getLogger(BedrockService.class);
    
    @Value("${aws.bedrock.region:us-east-1}")
    private String bedrockRegion;
    
    @Value("${aws.bedrock.model-id:anthropic.claude-3-sonnet-20240229-v1:0}")
    private String modelId;
    
    private BedrockRuntimeClient bedrockClient;
    private ObjectMapper objectMapper;
    
    public BedrockService() {
        this.objectMapper = new ObjectMapper();
    }
    
    private BedrockRuntimeClient getBedrockClient() {
        if (bedrockClient == null) {
            bedrockClient = BedrockRuntimeClient.builder()
                    .region(Region.of(bedrockRegion))
                    .credentialsProvider(DefaultCredentialsProvider.create())
                    .build();
        }
        return bedrockClient;
    }
    
    public CodeReviewResult analyzeCode(String code, String language) {
        try {
            logger.info("Starting Bedrock analysis for language: {}", language);
            
            String prompt = buildAnalysisPrompt(code, language);
            String response = invokeClaudeModel(prompt);
            
            return parseAnalysisResponse(response);
            
        } catch (Exception e) {
            logger.error("Error analyzing code with Bedrock", e);
            return createErrorResult(e.getMessage());
        }
    }
    
    private String buildAnalysisPrompt(String code, String language) {
        return String.format("""
            You are an expert code reviewer. Analyze the following %s code and provide a comprehensive review.
            
            Please analyze the code for:
            1. Security vulnerabilities
            2. Performance issues
            3. Code quality and maintainability
            4. Best practices adherence
            5. Potential bugs
            6. Suggestions for improvement
            
            Provide your response in JSON format with the following structure:
            {
              "summary": "Brief overview of the code quality",
              "overallScore": 8.5,
              "issues": [
                {
                  "severity": "HIGH",
                  "type": "SECURITY",
                  "title": "Issue title",
                  "description": "Detailed description",
                  "lineNumber": 15,
                  "codeSnippet": "problematic code",
                  "suggestion": "How to fix"
                }
              ],
              "suggestions": [
                {
                  "title": "Suggestion title",
                  "description": "Description",
                  "category": "Performance",
                  "impact": "High",
                  "implementation": "How to implement"
                }
              ],
              "security": {
                "securityScore": 7.5,
                "vulnerabilities": ["List of vulnerabilities"],
                "recommendations": ["Security recommendations"],
                "hasSecurityIssues": true
              },
              "performance": {
                "performanceScore": 8.0,
                "bottlenecks": ["Performance bottlenecks"],
                "optimizations": ["Optimization suggestions"],
                "complexity": "Medium"
              },
              "quality": {
                "maintainabilityScore": 8.5,
                "readabilityScore": 9.0,
                "linesOfCode": 150,
                "complexityScore": 3,
                "testCoverage": 0.0,
                "duplicateLines": 0
              }
            }
            
            Code to analyze:
            ```%s
            %s
            ```
            
            Respond only with valid JSON, no additional text.
            """, language, language, code);
    }
    
    private String invokeClaudeModel(String prompt) throws Exception {
        try {
            // Build request payload for Claude
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("anthropic_version", "bedrock-2023-05-31");
            requestBody.put("max_tokens", 4000);
            
            List<Map<String, Object>> messages = new ArrayList<>();
            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);
            messages.add(message);
            
            requestBody.put("messages", messages);
            
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            
            InvokeModelRequest request = InvokeModelRequest.builder()
                    .modelId(modelId)
                    .body(SdkBytes.fromUtf8String(jsonBody))
                    .contentType("application/json")
                    .accept("application/json")
                    .build();
            
            InvokeModelResponse response = getBedrockClient().invokeModel(request);
            String responseBody = response.body().asUtf8String();
            
            // Parse Claude response
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
            List<Map<String, Object>> content = (List<Map<String, Object>>) responseMap.get("content");
            
            if (content != null && !content.isEmpty()) {
                return (String) content.get(0).get("text");
            }
            
            throw new RuntimeException("Invalid response from Claude model");
            
        } catch (Exception e) {
            logger.error("Error invoking Claude model", e);
            throw e;
        }
    }
    
    private CodeReviewResult parseAnalysisResponse(String response) {
        try {
            // Clean up response if needed
            String cleanedResponse = response.trim();
            if (cleanedResponse.startsWith("```json")) {
                cleanedResponse = cleanedResponse.substring(7);
            }
            if (cleanedResponse.endsWith("```")) {
                cleanedResponse = cleanedResponse.substring(0, cleanedResponse.length() - 3);
            }
            
            Map<String, Object> responseMap = objectMapper.readValue(cleanedResponse, Map.class);
            
            return CodeReviewResult.builder()
                    .summary((String) responseMap.get("summary"))
                    .overallScore(((Number) responseMap.getOrDefault("overallScore", 5.0)).doubleValue())
                    .issues(parseIssues((List<Map<String, Object>>) responseMap.get("issues")))
                    .suggestions(parseSuggestions((List<Map<String, Object>>) responseMap.get("suggestions")))
                    .security(parseSecurityAnalysis((Map<String, Object>) responseMap.get("security")))
                    .performance(parsePerformanceAnalysis((Map<String, Object>) responseMap.get("performance")))
                    .quality(parseQualityMetrics((Map<String, Object>) responseMap.get("quality")))
                    .metadata(responseMap)
                    .build();
                    
        } catch (Exception e) {
            logger.error("Error parsing analysis response", e);
            return createErrorResult("Failed to parse AI response");
        }
    }
    
    private List<CodeIssue> parseIssues(List<Map<String, Object>> issuesData) {
        List<CodeIssue> issues = new ArrayList<>();
        
        if (issuesData != null) {
            for (Map<String, Object> issueData : issuesData) {
                CodeIssue issue = CodeIssue.builder()
                        .severity(IssueSeverity.valueOf((String) issueData.getOrDefault("severity", "MEDIUM")))
                        .type(IssueType.valueOf((String) issueData.getOrDefault("type", "CODE_SMELL")))
                        .title((String) issueData.get("title"))
                        .description((String) issueData.get("description"))
                        .lineNumber(((Number) issueData.getOrDefault("lineNumber", 0)).intValue())
                        .codeSnippet((String) issueData.get("codeSnippet"))
                        .suggestion((String) issueData.get("suggestion"))
                        .build();
                issues.add(issue);
            }
        }
        
        return issues;
    }
    
    private List<Suggestion> parseSuggestions(List<Map<String, Object>> suggestionsData) {
        List<Suggestion> suggestions = new ArrayList<>();
        
        if (suggestionsData != null) {
            for (Map<String, Object> suggestionData : suggestionsData) {
                Suggestion suggestion = Suggestion.builder()
                        .title((String) suggestionData.get("title"))
                        .description((String) suggestionData.get("description"))
                        .category((String) suggestionData.get("category"))
                        .impact((String) suggestionData.get("impact"))
                        .implementation((String) suggestionData.get("implementation"))
                        .build();
                suggestions.add(suggestion);
            }
        }
        
        return suggestions;
    }
    
    private SecurityAnalysis parseSecurityAnalysis(Map<String, Object> securityData) {
        if (securityData == null) {
            return SecurityAnalysis.builder()
                    .securityScore(5.0)
                    .vulnerabilities(new ArrayList<>())
                    .recommendations(new ArrayList<>())
                    .hasSecurityIssues(false)
                    .build();
        }
        
        return SecurityAnalysis.builder()
                .securityScore(((Number) securityData.getOrDefault("securityScore", 5.0)).doubleValue())
                .vulnerabilities((List<String>) securityData.getOrDefault("vulnerabilities", new ArrayList<>()))
                .recommendations((List<String>) securityData.getOrDefault("recommendations", new ArrayList<>()))
                .hasSecurityIssues((Boolean) securityData.getOrDefault("hasSecurityIssues", false))
                .build();
    }
    
    private PerformanceAnalysis parsePerformanceAnalysis(Map<String, Object> performanceData) {
        if (performanceData == null) {
            return PerformanceAnalysis.builder()
                    .performanceScore(5.0)
                    .bottlenecks(new ArrayList<>())
                    .optimizations(new ArrayList<>())
                    .complexity("Medium")
                    .build();
        }
        
        return PerformanceAnalysis.builder()
                .performanceScore(((Number) performanceData.getOrDefault("performanceScore", 5.0)).doubleValue())
                .bottlenecks((List<String>) performanceData.getOrDefault("bottlenecks", new ArrayList<>()))
                .optimizations((List<String>) performanceData.getOrDefault("optimizations", new ArrayList<>()))
                .complexity((String) performanceData.getOrDefault("complexity", "Medium"))
                .build();
    }
    
    private QualityMetrics parseQualityMetrics(Map<String, Object> qualityData) {
        if (qualityData == null) {
            return QualityMetrics.builder()
                    .maintainabilityScore(5.0)
                    .readabilityScore(5.0)
                    .linesOfCode(0)
                    .complexityScore(3)
                    .testCoverage(0.0)
                    .duplicateLines(0)
                    .build();
        }
        
        return QualityMetrics.builder()
                .maintainabilityScore(((Number) qualityData.getOrDefault("maintainabilityScore", 5.0)).doubleValue())
                .readabilityScore(((Number) qualityData.getOrDefault("readabilityScore", 5.0)).doubleValue())
                .linesOfCode(((Number) qualityData.getOrDefault("linesOfCode", 0)).intValue())
                .complexityScore(((Number) qualityData.getOrDefault("complexityScore", 3)).intValue())
                .testCoverage(((Number) qualityData.getOrDefault("testCoverage", 0.0)).doubleValue())
                .duplicateLines(((Number) qualityData.getOrDefault("duplicateLines", 0)).intValue())
                .build();
    }
    
    private CodeReviewResult createErrorResult(String errorMessage) {
        return CodeReviewResult.builder()
                .summary("Analysis failed: " + errorMessage)
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
                .metadata(new HashMap<>())
                .build();
    }
}



