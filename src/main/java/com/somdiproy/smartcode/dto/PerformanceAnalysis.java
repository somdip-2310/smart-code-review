package com.somdiproy.smartcode.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Performance Analysis Result DTO
 * Contains performance assessment and optimization recommendations
 * 
 * @author Somdip Roy
 */
public class PerformanceAnalysis {
    
    private double performanceScore;        // 0-10 scale
    private List<String> bottlenecks;       // List of performance bottlenecks
    private List<String> optimizations;     // Optimization recommendations
    private String complexity;              // O(n), O(n²), etc.
    private int inefficientLoopsCount;
    private int unnecessaryOperationsCount;
    private List<PerformanceIssue> issues;
    private String memoryAnalysis;
    private String timeComplexityAnalysis;
    private String spaceComplexityAnalysis;
    
    // Default constructor
    public PerformanceAnalysis() {
        this.bottlenecks = new ArrayList<>();
        this.optimizations = new ArrayList<>();
        this.issues = new ArrayList<>();
    }
    
    // Builder pattern
    public static PerformanceAnalysisBuilder builder() {
        return new PerformanceAnalysisBuilder();
    }
    
    // Getters and Setters
    public double getPerformanceScore() {
        return performanceScore;
    }
    
    public void setPerformanceScore(double performanceScore) {
        this.performanceScore = performanceScore;
    }
    
    public List<String> getBottlenecks() {
        return bottlenecks;
    }
    
    public void setBottlenecks(List<String> bottlenecks) {
        this.bottlenecks = bottlenecks;
    }
    
    public List<String> getOptimizations() {
        return optimizations;
    }
    
    public void setOptimizations(List<String> optimizations) {
        this.optimizations = optimizations;
    }
    
    public String getComplexity() {
        return complexity;
    }
    
    public void setComplexity(String complexity) {
        this.complexity = complexity;
    }
    
    public int getInefficientLoopsCount() {
        return inefficientLoopsCount;
    }
    
    public void setInefficientLoopsCount(int inefficientLoopsCount) {
        this.inefficientLoopsCount = inefficientLoopsCount;
    }
    
    public int getUnnecessaryOperationsCount() {
        return unnecessaryOperationsCount;
    }
    
    public void setUnnecessaryOperationsCount(int unnecessaryOperationsCount) {
        this.unnecessaryOperationsCount = unnecessaryOperationsCount;
    }
    
    public List<PerformanceIssue> getIssues() {
        return issues;
    }
    
    public void setIssues(List<PerformanceIssue> issues) {
        this.issues = issues;
    }
    
    public String getMemoryAnalysis() {
        return memoryAnalysis;
    }
    
    public void setMemoryAnalysis(String memoryAnalysis) {
        this.memoryAnalysis = memoryAnalysis;
    }
    
    public String getTimeComplexityAnalysis() {
        return timeComplexityAnalysis;
    }
    
    public void setTimeComplexityAnalysis(String timeComplexityAnalysis) {
        this.timeComplexityAnalysis = timeComplexityAnalysis;
    }
    
    public String getSpaceComplexityAnalysis() {
        return spaceComplexityAnalysis;
    }
    
    public void setSpaceComplexityAnalysis(String spaceComplexityAnalysis) {
        this.spaceComplexityAnalysis = spaceComplexityAnalysis;
    }
    
    // Inner class for performance issues
    public static class PerformanceIssue {
        private String type;            // N+1 Query, Memory Leak, etc.
        private String severity;        // HIGH, MEDIUM, LOW
        private String location;
        private String description;
        private String solution;
        private String estimatedImpact; // e.g., "50% faster"
        
        // Getters and Setters
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public String getSeverity() {
            return severity;
        }
        
        public void setSeverity(String severity) {
            this.severity = severity;
        }
        
        public String getLocation() {
            return location;
        }
        
        public void setLocation(String location) {
            this.location = location;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public String getSolution() {
            return solution;
        }
        
        public void setSolution(String solution) {
            this.solution = solution;
        }
        
        public String getEstimatedImpact() {
            return estimatedImpact;
        }
        
        public void setEstimatedImpact(String estimatedImpact) {
            this.estimatedImpact = estimatedImpact;
        }
    }
    
    // Builder class
    public static class PerformanceAnalysisBuilder {
        private double performanceScore;
        private List<String> bottlenecks = new ArrayList<>();
        private List<String> optimizations = new ArrayList<>();
        private String complexity;
        private int inefficientLoopsCount;
        private int unnecessaryOperationsCount;
        private List<PerformanceIssue> issues = new ArrayList<>();
        private String memoryAnalysis;
        private String timeComplexityAnalysis;
        private String spaceComplexityAnalysis;
        
        public PerformanceAnalysisBuilder performanceScore(double performanceScore) {
            this.performanceScore = performanceScore;
            return this;
        }
        
        public PerformanceAnalysisBuilder bottlenecks(List<String> bottlenecks) {
            this.bottlenecks = bottlenecks;
            return this;
        }
        
        public PerformanceAnalysisBuilder optimizations(List<String> optimizations) {
            this.optimizations = optimizations;
            return this;
        }
        
        public PerformanceAnalysisBuilder complexity(String complexity) {
            this.complexity = complexity;
            return this;
        }
        
        public PerformanceAnalysisBuilder inefficientLoopsCount(int inefficientLoopsCount) {
            this.inefficientLoopsCount = inefficientLoopsCount;
            return this;
        }
        
        public PerformanceAnalysisBuilder unnecessaryOperationsCount(int unnecessaryOperationsCount) {
            this.unnecessaryOperationsCount = unnecessaryOperationsCount;
            return this;
        }
        
        public PerformanceAnalysisBuilder issues(List<PerformanceIssue> issues) {
            this.issues = issues;
            return this;
        }
        
        public PerformanceAnalysisBuilder memoryAnalysis(String memoryAnalysis) {
            this.memoryAnalysis = memoryAnalysis;
            return this;
        }
        
        public PerformanceAnalysisBuilder timeComplexityAnalysis(String timeComplexityAnalysis) {
            this.timeComplexityAnalysis = timeComplexityAnalysis;
            return this;
        }
        
        public PerformanceAnalysisBuilder spaceComplexityAnalysis(String spaceComplexityAnalysis) {
            this.spaceComplexityAnalysis = spaceComplexityAnalysis;
            return this;
        }
        
        public PerformanceAnalysis build() {
            PerformanceAnalysis analysis = new PerformanceAnalysis();
            analysis.performanceScore = this.performanceScore;
            analysis.bottlenecks = this.bottlenecks;
            analysis.optimizations = this.optimizations;
            analysis.complexity = this.complexity;
            analysis.inefficientLoopsCount = this.inefficientLoopsCount;
            analysis.unnecessaryOperationsCount = this.unnecessaryOperationsCount;
            analysis.issues = this.issues;
            analysis.memoryAnalysis = this.memoryAnalysis;
            analysis.timeComplexityAnalysis = this.timeComplexityAnalysis;
            analysis.spaceComplexityAnalysis = this.spaceComplexityAnalysis;
            return analysis;
        }
    }
}