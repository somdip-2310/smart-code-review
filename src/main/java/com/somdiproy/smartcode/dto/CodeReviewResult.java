package com.somdiproy.smartcode.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

/**
 * Code Review Result DTO
 * 
 * Contains the complete analysis results from AI
 * 
 * @author Somdip Roy
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeReviewResult {
    
    /**
     * Summary of the analysis
     */
    private String summary;
    
    /**
     * Overall score (0-10)
     */
    private Double overallScore;
    
    /**
     * List of identified issues
     */
    private List<CodeIssue> issues;
    
    /**
     * List of improvement suggestions
     */
    private List<Suggestion> suggestions;
    
    /**
     * Security analysis results
     */
    private SecurityAnalysis security;
    
    /**
     * Performance analysis results
     */
    private PerformanceAnalysis performance;
    
    /**
     * Code quality metrics
     */
    private QualityMetrics quality;
    
    /**
     * Additional metadata from AI analysis
     */
    private Map<String, Object> metadata;
    
    /**
     * Analysis timestamp
     */
    private Long timestamp;
}