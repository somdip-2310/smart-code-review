package com.somdiproy.smartcode.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * Code Quality Metrics DTO
 * Contains various code quality measurements
 * 
 * @author Somdip Roy
 */
public class QualityMetrics {
    
    private double maintainabilityScore;    // 0-10 scale
    private double readabilityScore;        // 0-10 scale
    private int linesOfCode;
    private int complexityScore;            // Cyclomatic complexity
    private double testCoverage;            // Percentage (0-100)
    private int duplicateLines;
    private int commentedLines;
    private double documentationCoverage;   // Percentage
    private int codeSmellsCount;
    private int technicalDebtMinutes;       // Estimated time to fix all issues
    private Map<String, Integer> metricBreakdown;
    
    // Default constructor
    public QualityMetrics() {
        this.metricBreakdown = new HashMap<>();
    }
    
    // Builder pattern
    public static QualityMetricsBuilder builder() {
        return new QualityMetricsBuilder();
    }
    
    // Calculate overall quality score
    public double getOverallQualityScore() {
        return (maintainabilityScore + readabilityScore) / 2.0;
    }
    
    // Getters and Setters
    public double getMaintainabilityScore() {
        return maintainabilityScore;
    }
    
    public void setMaintainabilityScore(double maintainabilityScore) {
        this.maintainabilityScore = maintainabilityScore;
    }
    
    public double getReadabilityScore() {
        return readabilityScore;
    }
    
    public void setReadabilityScore(double readabilityScore) {
        this.readabilityScore = readabilityScore;
    }
    
    public int getLinesOfCode() {
        return linesOfCode;
    }
    
    public void setLinesOfCode(int linesOfCode) {
        this.linesOfCode = linesOfCode;
    }
    
    public int getComplexityScore() {
        return complexityScore;
    }
    
    public void setComplexityScore(int complexityScore) {
        this.complexityScore = complexityScore;
    }
    
    public double getTestCoverage() {
        return testCoverage;
    }
    
    public void setTestCoverage(double testCoverage) {
        this.testCoverage = testCoverage;
    }
    
    public int getDuplicateLines() {
        return duplicateLines;
    }
    
    public void setDuplicateLines(int duplicateLines) {
        this.duplicateLines = duplicateLines;
    }
    
    public int getCommentedLines() {
        return commentedLines;
    }
    
    public void setCommentedLines(int commentedLines) {
        this.commentedLines = commentedLines;
    }
    
    public double getDocumentationCoverage() {
        return documentationCoverage;
    }
    
    public void setDocumentationCoverage(double documentationCoverage) {
        this.documentationCoverage = documentationCoverage;
    }
    
    public int getCodeSmellsCount() {
        return codeSmellsCount;
    }
    
    public void setCodeSmellsCount(int codeSmellsCount) {
        this.codeSmellsCount = codeSmellsCount;
    }
    
    public int getTechnicalDebtMinutes() {
        return technicalDebtMinutes;
    }
    
    public void setTechnicalDebtMinutes(int technicalDebtMinutes) {
        this.technicalDebtMinutes = technicalDebtMinutes;
    }
    
    public Map<String, Integer> getMetricBreakdown() {
        return metricBreakdown;
    }
    
    public void setMetricBreakdown(Map<String, Integer> metricBreakdown) {
        this.metricBreakdown = metricBreakdown;
    }
    
    // Builder class
    public static class QualityMetricsBuilder {
        private double maintainabilityScore;
        private double readabilityScore;
        private int linesOfCode;
        private int complexityScore;
        private double testCoverage;
        private int duplicateLines;
        private int commentedLines;
        private double documentationCoverage;
        private int codeSmellsCount;
        private int technicalDebtMinutes;
        private Map<String, Integer> metricBreakdown = new HashMap<>();
        
        public QualityMetricsBuilder maintainabilityScore(double maintainabilityScore) {
            this.maintainabilityScore = maintainabilityScore;
            return this;
        }
        
        public QualityMetricsBuilder readabilityScore(double readabilityScore) {
            this.readabilityScore = readabilityScore;
            return this;
        }
        
        public QualityMetricsBuilder linesOfCode(int linesOfCode) {
            this.linesOfCode = linesOfCode;
            return this;
        }
        
        public QualityMetricsBuilder complexityScore(int complexityScore) {
            this.complexityScore = complexityScore;
            return this;
        }
        
        public QualityMetricsBuilder testCoverage(double testCoverage) {
            this.testCoverage = testCoverage;
            return this;
        }
        
        public QualityMetricsBuilder duplicateLines(int duplicateLines) {
            this.duplicateLines = duplicateLines;
            return this;
        }
        
        public QualityMetricsBuilder commentedLines(int commentedLines) {
            this.commentedLines = commentedLines;
            return this;
        }
        
        public QualityMetricsBuilder documentationCoverage(double documentationCoverage) {
            this.documentationCoverage = documentationCoverage;
            return this;
        }
        
        public QualityMetricsBuilder codeSmellsCount(int codeSmellsCount) {
            this.codeSmellsCount = codeSmellsCount;
            return this;
        }
        
        public QualityMetricsBuilder technicalDebtMinutes(int technicalDebtMinutes) {
            this.technicalDebtMinutes = technicalDebtMinutes;
            return this;
        }
        
        public QualityMetricsBuilder metricBreakdown(Map<String, Integer> metricBreakdown) {
            this.metricBreakdown = metricBreakdown;
            return this;
        }
        
        public QualityMetrics build() {
            QualityMetrics metrics = new QualityMetrics();
            metrics.maintainabilityScore = this.maintainabilityScore;
            metrics.readabilityScore = this.readabilityScore;
            metrics.linesOfCode = this.linesOfCode;
            metrics.complexityScore = this.complexityScore;
            metrics.testCoverage = this.testCoverage;
            metrics.duplicateLines = this.duplicateLines;
            metrics.commentedLines = this.commentedLines;
            metrics.documentationCoverage = this.documentationCoverage;
            metrics.codeSmellsCount = this.codeSmellsCount;
            metrics.technicalDebtMinutes = this.technicalDebtMinutes;
            metrics.metricBreakdown = this.metricBreakdown;
            return metrics;
        }
    }
}