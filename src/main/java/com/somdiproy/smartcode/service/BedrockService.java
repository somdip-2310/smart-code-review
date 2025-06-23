package com.somdiproy.smartcode.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.somdiproy.smartcode.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;
import software.amazon.awssdk.core.retry.conditions.RetryCondition;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Amazon Bedrock Service for Smart Code Review
 * 
 * Integrates with Amazon Bedrock to provide AI-powered code analysis using
 * Nova Premier. Handles code review requests, prompt engineering, and
 * response parsing to deliver comprehensive code analysis results.
 * 
 * Features:
 * - Claude 3 Sonnet integration
 * - Structured JSON response parsing
 * - Error handling and fallbacks
 * - Connection pooling and optimization
 * 
 * @author Somdip Roy
 * @version 1.0.0
 */
@Service
public class BedrockService {
    
    private static final Logger logger = LoggerFactory.getLogger(BedrockService.class);
    
    @Value("${aws.bedrock.region:us-east-1}")
    private String bedrockRegion;
    
    @Value("${aws.bedrock.model-id:anthropic.claude-3-sonnet-20240229-v1:0}")
    private String modelId;
    
    @Value("${analysis.timeout.seconds:120}")
    private int analysisTimeoutSeconds;
    
    private BedrockRuntimeClient bedrockClient;
    private ObjectMapper objectMapper;
    
    private final BedrockRequestQueue requestQueue;
    private final BedrockCircuitBreaker circuitBreaker;

    private final CodeChunkingService codeChunkingService;

    public BedrockService(BedrockRequestQueue requestQueue, 
                         BedrockCircuitBreaker circuitBreaker,
                         CodeChunkingService codeChunkingService) {
        this.requestQueue = requestQueue;
        this.circuitBreaker = circuitBreaker;
        this.codeChunkingService = codeChunkingService;
    }
    
    @PostConstruct
    public void init() {
        this.objectMapper = new ObjectMapper();
        logger.info("BedrockService initialized with model: {} in region: {}", modelId, bedrockRegion);
    }
    
    @PreDestroy
    public void cleanup() {
        if (bedrockClient != null) {
            bedrockClient.close();
            if (requestQueue != null) {
                requestQueue.shutdown();
            }
            logger.info("BedrockService cleanup completed");
        }
    }
    
    /**
     * Get or create Bedrock Runtime Client
     */
    private BedrockRuntimeClient getBedrockClient() {
        if (bedrockClient == null) {
            try {
                // Configure retry policy with exponential backoff
                RetryPolicy retryPolicy = RetryPolicy.builder()
                        .numRetries(5) // Increase retry attempts
                        .retryCondition(RetryCondition.defaultRetryCondition())
                        .backoffStrategy(BackoffStrategy.defaultThrottlingStrategy()) // Uses exponential backoff
                        .throttlingBackoffStrategy(BackoffStrategy.defaultThrottlingStrategy())
                        .build();
                
                // Configure client with custom retry and timeout settings
                bedrockClient = BedrockRuntimeClient.builder()
                        .region(Region.of(bedrockRegion))
                        .credentialsProvider(DefaultCredentialsProvider.create())
                        .overrideConfiguration(ClientOverrideConfiguration.builder()
                                .retryPolicy(retryPolicy)
                                .apiCallTimeout(Duration.ofSeconds(analysisTimeoutSeconds))
                                .apiCallAttemptTimeout(Duration.ofSeconds(30))
                                .build())
                        .build();
                logger.info("BedrockRuntimeClient created successfully with retry configuration");
            } catch (Exception e) {
                logger.error("Failed to create BedrockRuntimeClient", e);
                throw new RuntimeException("Failed to initialize Bedrock client", e);
            }
        }
        return bedrockClient;
    }
    
    /**
     * Analyze code using Amazon Bedrock Claude model
     */
    public CodeReviewResult analyzeCode(String code, String language) {
        try {
            // Check circuit breaker
            if (!circuitBreaker.allowRequest()) {
                logger.warn("Circuit breaker is open. Rejecting analysis request.");
                return createErrorResult("Service temporarily unavailable. Please try again later.");
            }
            
            logger.info("Starting Bedrock analysis for language: {} (code length: {})", language, code.length());
            
            // Validate input
            if (code == null || code.trim().isEmpty()) {
                return createErrorResult("Code content is empty");
            }
            
            if (language == null || language.trim().isEmpty()) {
                language = "unknown";
            }
            
            // Check if code needs chunking
            if (!codeChunkingService.isWithinTokenLimit(code)) {
                logger.info("Code exceeds token limit, using chunked analysis");
                return analyzeCodeInChunks(code, language);
            }
            
            // Single chunk analysis
            String prompt = buildAnalysisPrompt(code, language);
            
            // Submit request through queue for rate limiting
            CompletableFuture<String> responseFuture = requestQueue.submitRequest(() -> invokeNovaModel(prompt));
            String response = responseFuture.get(analysisTimeoutSeconds, TimeUnit.SECONDS);
            
            // Parse and return results
            CodeReviewResult result = parseAnalysisResponse(response);
            
            logger.info("Analysis completed successfully for language: {}", language);
            circuitBreaker.recordSuccess();
            return result;
            
        } catch (Exception e) {
            logger.error("Error analyzing code with Bedrock", e);
            circuitBreaker.recordFailure();
            return createErrorResult("Analysis failed: " + e.getMessage());
        }
    }

    private CodeReviewResult analyzeCodeInChunks(String code, String language) {
        try {
            List<CodeChunkingService.CodeChunk> chunks = codeChunkingService.chunkCode(code, "main");
            logger.info("Analyzing {} code chunks", chunks.size());
            
            List<CodeReviewResult> chunkResults = new ArrayList<>();
            
            for (int i = 0; i < chunks.size(); i++) {
                CodeChunkingService.CodeChunk chunk = chunks.get(i);
                logger.info("Analyzing chunk {}/{} (lines {}-{}, {} tokens)", 
                    i + 1, chunks.size(), chunk.getStartLine(), chunk.getEndLine(), chunk.getEstimatedTokens());
                
                String chunkPrompt = buildChunkAnalysisPrompt(chunk, language, i + 1, chunks.size());
                
                try {
                    CompletableFuture<String> responseFuture = requestQueue.submitRequest(() -> invokeNovaModel(chunkPrompt));
                    String response = responseFuture.get(analysisTimeoutSeconds, TimeUnit.SECONDS);
                    
                    CodeReviewResult chunkResult = parseAnalysisResponse(response);
                    chunkResults.add(chunkResult);
                    
                    // Add delay between chunks to avoid rate limiting
                    if (i < chunks.size() - 1) {
                        Thread.sleep(2000); // 2 second delay between chunks
                    }
                    
                } catch (Exception e) {
                    logger.error("Error analyzing chunk {}: {}", i + 1, e.getMessage());
                    // Continue with other chunks
                }
            }
            
            // Merge results from all chunks
            return mergeChunkResults(chunkResults);
            
        } catch (Exception e) {
            logger.error("Error in chunked analysis", e);
            return createErrorResult("Chunked analysis failed: " + e.getMessage());
        }
    }

    private String buildChunkAnalysisPrompt(CodeChunkingService.CodeChunk chunk, String language, int chunkNumber, int totalChunks) {
        return String.format("""
            You are analyzing chunk %d of %d from a %s code file.
            Lines %d-%d of the file are shown below.
            
            Analyze this code chunk for:
            1. Security vulnerabilities
            2. Performance issues
            3. Code quality problems
            4. Best practices violations
            5. Potential bugs
            
            Note: This is a partial code analysis. Some issues may require full context.
            
            Provide your response in JSON format following the standard structure.
            
            Code chunk:
            ```%s
            %s
            ```
            
            Respond only with valid JSON, no additional text or markdown formatting.
            """, chunkNumber, totalChunks, language, chunk.getStartLine(), chunk.getEndLine(), 
            language, chunk.getContent());
    }

    private CodeReviewResult mergeChunkResults(List<CodeReviewResult> chunkResults) {
        if (chunkResults.isEmpty()) {
            return createErrorResult("No chunk results to merge");
        }
        
        // Aggregate all issues, suggestions, etc. from chunks
        List<Issue> allIssues = new ArrayList<>();
        List<Suggestion> allSuggestions = new ArrayList<>();
        List<String> allVulnerabilities = new ArrayList<>();
        List<String> allBottlenecks = new ArrayList<>();
        List<String> allOptimizations = new ArrayList<>();
        List<String> allRecommendations = new ArrayList<>();
        
        double totalScore = 0;
        int validScores = 0;
        
        for (CodeReviewResult result : chunkResults) {
            if (result.getIssues() != null) allIssues.addAll(result.getIssues());
            if (result.getSuggestions() != null) allSuggestions.addAll(result.getSuggestions());
            if (result.getSecurity() != null && result.getSecurity().getVulnerabilities() != null) {
                allVulnerabilities.addAll(result.getSecurity().getVulnerabilities());
            }
            if (result.getPerformance() != null) {
                if (result.getPerformance().getBottlenecks() != null) {
                    allBottlenecks.addAll(result.getPerformance().getBottlenecks());
                }
                if (result.getPerformance().getOptimizations() != null) {
                    allOptimizations.addAll(result.getPerformance().getOptimizations());
                }
            }
            if (result.getSecurity() != null && result.getSecurity().getRecommendations() != null) {
                allRecommendations.addAll(result.getSecurity().getRecommendations());
            }
            
            if (result.getOverallScore() > 0) {
                totalScore += result.getOverallScore();
                validScores++;
            }
        }
        
        double averageScore = validScores > 0 ? totalScore / validScores : 5.0;
        
        return CodeReviewResult.builder()
                .summary("Comprehensive analysis completed across " + chunkResults.size() + " code chunks")
                .overallScore(averageScore)
                .issues(allIssues)
                .suggestions(allSuggestions)
                .security(SecurityAnalysis.builder()
                        .securityScore(averageScore)
                        .vulnerabilities(allVulnerabilities)
                        .recommendations(allRecommendations)
                        .hasSecurityIssues(!allVulnerabilities.isEmpty())
                        .build())
                .performance(PerformanceAnalysis.builder()
                        .performanceScore(averageScore)
                        .bottlenecks(allBottlenecks)
                        .optimizations(allOptimizations)
                        .complexity("Complex")
                        .build())
                .quality(QualityMetrics.builder()
                        .maintainabilityScore(averageScore)
                        .readabilityScore(averageScore)
                        .linesOfCode(0)
                        .complexityScore(5)
                        .testCoverage(0.0)
                        .duplicateLines(0)
                        .build())
                .metadata(new HashMap<>())
                .build();
    }
    
    /**
     * Build comprehensive analysis prompt for Claude
     */
    private String buildAnalysisPrompt(String code, String language) {
        return String.format("""
            You are an expert code reviewer with 15+ years of experience. Analyze the following %s code and provide a comprehensive review.
            
            Please analyze the code for:
            1. Security vulnerabilities and potential attack vectors
            2. Performance issues and optimization opportunities
            3. Code quality, maintainability, and readability
            4. Best practices adherence and design patterns
            5. Potential bugs and logic errors
            6. Suggestions for improvement and refactoring
            
            Provide your response in JSON format with the following structure:
            {
              "summary": "Brief overview of the code quality and main findings",
              "overallScore": 8.5,
              "issues": [
                {
                  "severity": "HIGH",
                  "type": "SECURITY",
                  "title": "Issue title",
                  "description": "Detailed description of the issue",
                  "lineNumber": 15,
                  "codeSnippet": "problematic code snippet",
                  "suggestion": "How to fix this issue"
                }
              ],
              "suggestions": [
                {
                  "title": "Suggestion title",
                  "description": "Detailed description of the improvement",
                  "category": "Performance",
                  "impact": "High",
                  "implementation": "Step-by-step implementation guide"
                }
              ],
              "security": {
                "securityScore": 7.5,
                "vulnerabilities": ["List of security vulnerabilities found"],
                "recommendations": ["Security improvement recommendations"],
                "hasSecurityIssues": true
              },
              "performance": {
                "performanceScore": 8.0,
                "bottlenecks": ["Performance bottlenecks identified"],
                "optimizations": ["Performance optimization suggestions"],
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
            
            Respond only with valid JSON, no additional text or markdown formatting.
            """, language, language, code);
    }
    
    /**
     * Invoke Nova Premier model via Bedrock Runtime
     */
    private String invokeNovaModel(String prompt) throws Exception {
        try {
            // Build request payload for Nova Premier
            Map<String, Object> requestBody = new HashMap<>();
            
            // Nova uses a different message format
            List<Map<String, Object>> messages = new ArrayList<>();
            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            
            // Nova expects content as an array
            List<Map<String, Object>> contentArray = new ArrayList<>();
            Map<String, Object> textContent = new HashMap<>();
            textContent.put("text", prompt);
            contentArray.add(textContent);
            
            message.put("content", contentArray);
            messages.add(message);
            
            requestBody.put("messages", messages);
            
            // Nova inference configuration
            Map<String, Object> inferenceConfig = new HashMap<>();
            inferenceConfig.put("maxTokens", 4000);
            inferenceConfig.put("temperature", 0.1);
            inferenceConfig.put("topP", 0.9);
            
            requestBody.put("inferenceConfig", inferenceConfig);
            
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            
            // Create invoke request
            InvokeModelRequest request = InvokeModelRequest.builder()
                    .modelId(modelId)
                    .body(SdkBytes.fromUtf8String(jsonBody))
                    .contentType("application/json")
                    .accept("application/json")
                    .build();
            
            // Invoke model
            logger.debug("Invoking model: {}", modelId);
            InvokeModelResponse response = getBedrockClient().invokeModel(request);
            String responseBody = response.body().asUtf8String();
            
            
            // Parse Nova response
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);

            // Nova output
            Map<String, Object> output = (Map<String, Object>) responseMap.get("output");
            if (output != null) {
                Map<String, Object> outputMessage = (Map<String, Object>) output.get("message");
                if (outputMessage != null) {
                    List<Map<String, Object>> content = (List<Map<String, Object>>) outputMessage.get("content");
                    if (content != null && !content.isEmpty()) {
                        return (String) content.get(0).get("text");
                    }
                }
            }
            
            throw new RuntimeException("Invalid response from Nova model");
            
        } catch (Exception e) {
            logger.error("Error invoking Nova model", e);
            throw new Exception("Failed to analyze code: " + e.getMessage(), e);
        }
    }
    
    /**
     * Parse Claude's JSON response into structured CodeReviewResult
     */
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
            return createErrorResult("Failed to parse AI response: " + e.getMessage());
        }
    }
    
    /**
     * Parse issues from response
     */
    /**
     * Parse issues from response
     */
    private List<Issue> parseIssues(List<Map<String, Object>> issuesData) {
        List<Issue> issues = new ArrayList<>();
        
        if (issuesData != null) {
            for (Map<String, Object> issueData : issuesData) {
                try {
                    Issue issue = Issue.builder()
                            .severity(issueData.getOrDefault("severity", "MEDIUM").toString())
                            .type(issueData.getOrDefault("type", "CODE_SMELL").toString())
                            .title((String) issueData.get("title"))
                            .description((String) issueData.get("description"))
                            .lineNumber(((Number) issueData.getOrDefault("lineNumber", 0)).intValue())
                            .codeSnippet((String) issueData.get("codeSnippet"))
                            .suggestion((String) issueData.get("suggestion"))
                            .build();
                    issues.add(issue);
                } catch (Exception e) {
                    logger.warn("Error parsing issue data: {}", issueData, e);
                }
            }
        }
        
        return issues;
    }
    
    /**
     * Parse suggestions from response
     */
    private List<Suggestion> parseSuggestions(List<Map<String, Object>> suggestionsData) {
        List<Suggestion> suggestions = new ArrayList<>();
        
        if (suggestionsData != null) {
            for (Map<String, Object> suggestionData : suggestionsData) {
                try {
                    Suggestion suggestion = Suggestion.builder()
                            .title((String) suggestionData.get("title"))
                            .description((String) suggestionData.get("description"))
                            .category((String) suggestionData.get("category"))
                            .impact((String) suggestionData.get("impact"))
                            .implementation((String) suggestionData.get("implementation"))
                            .build();
                    suggestions.add(suggestion);
                } catch (Exception e) {
                    logger.warn("Error parsing suggestion data: {}", suggestionData, e);
                }
            }
        }
        
        return suggestions;
    }
    
    /**
     * Parse security analysis from response
     */
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
    
    /**
     * Parse performance analysis from response
     */
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
    
    /**
     * Parse quality metrics from response
     */
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
    
    /**
     * Create error result for failed analysis
     */
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
    
    /**
     * Health check for Bedrock connectivity
     */
    public boolean isHealthy() {
        try {
            // Simple health check - attempt to get client
            getBedrockClient();
            return true;
        } catch (Exception e) {
            logger.error("Bedrock health check failed", e);
            return false;
        }
    }
    
    /**
     * Get service information
     */
    public Map<String, Object> getServiceInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("modelId", modelId);
        info.put("region", bedrockRegion);
        info.put("timeoutSeconds", analysisTimeoutSeconds);
        info.put("healthy", isHealthy());
        info.put("timestamp", System.currentTimeMillis());
        return info;
    }
}