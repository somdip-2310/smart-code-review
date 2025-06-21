package com.somdiproy.smartcode.dto;

import java.util.List;
import java.util.Map;

/**
 * Code Review Result DTO
 * 
 * @author Somdip Roy
 */
public class CodeReviewResult {
    
    private String summary;
    private double overallScore;
    private List<Issue> issues;
    private List<Suggestion> suggestions;
    private SecurityAnalysis security;
    private PerformanceAnalysis performance;
    private QualityMetrics quality;
    private Map<String, Object> metadata;
    
    // Default constructor
    public CodeReviewResult() {
    }
    
    // Builder pattern implementation
    public static CodeReviewResultBuilder builder() {
        return new CodeReviewResultBuilder();
    }
    
    // Getters and Setters
    public String getSummary() {
        return summary;
    }
    
    public void setSummary(String summary) {
        this.summary = summary;
    }
    
    public double getOverallScore() {
        return overallScore;
    }
    
    public void setOverallScore(double overallScore) {
        this.overallScore = overallScore;
    }
    
    public List<Issue> getIssues() {
        return issues;
    }
    
    public void setIssues(List<Issue> issues) {
        this.issues = issues;
    }
    
    public List<Suggestion> getSuggestions() {
        return suggestions;
    }
    
    public void setSuggestions(List<Suggestion> suggestions) {
        this.suggestions = suggestions;
    }
    
    public SecurityAnalysis getSecurity() {
        return security;
    }
    
    public void setSecurity(SecurityAnalysis security) {
        this.security = security;
    }
    
    public PerformanceAnalysis getPerformance() {
        return performance;
    }
    
    public void setPerformance(PerformanceAnalysis performance) {
        this.performance = performance;
    }
    
    public QualityMetrics getQuality() {
        return quality;
    }
    
    public void setQuality(QualityMetrics quality) {
        this.quality = quality;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    // Builder class
    public static class CodeReviewResultBuilder {
        private String summary;
        private double overallScore;
        private List<Issue> issues;
        private List<Suggestion> suggestions;
        private SecurityAnalysis security;
        private PerformanceAnalysis performance;
        private QualityMetrics quality;
        private Map<String, Object> metadata;
        
        public CodeReviewResultBuilder summary(String summary) {
            this.summary = summary;
            return this;
        }
        
        public CodeReviewResultBuilder overallScore(double overallScore) {
            this.overallScore = overallScore;
            return this;
        }
        
        public CodeReviewResultBuilder issues(List<Issue> issues) {
            this.issues = issues;
            return this;
        }
        
        public CodeReviewResultBuilder suggestions(List<Suggestion> suggestions) {
            this.suggestions = suggestions;
            return this;
        }
        
        public CodeReviewResultBuilder security(SecurityAnalysis security) {
            this.security = security;
            return this;
        }
        
        public CodeReviewResultBuilder performance(PerformanceAnalysis performance) {
            this.performance = performance;
            return this;
        }
        
        public CodeReviewResultBuilder quality(QualityMetrics quality) {
            this.quality = quality;
            return this;
        }
        
        public CodeReviewResultBuilder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }
        
        public CodeReviewResult build() {
            CodeReviewResult result = new CodeReviewResult();
            result.summary = this.summary;
            result.overallScore = this.overallScore;
            result.issues = this.issues;
            result.suggestions = this.suggestions;
            result.security = this.security;
            result.performance = this.performance;
            result.quality = this.quality;
            result.metadata = this.metadata;
            return result;
        }
    }
}