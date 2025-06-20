package com.somdiproy.smartcode.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class CodeAnalysisResult {
    
    private String analysisId;
    private String sessionId;
    private AnalysisStatus status;
    private String overallScore;
    private String summary;
    private List<Finding> findings;
    private Map<String, Object> metrics;
    private List<Suggestion> suggestions;
    private LocalDateTime completedAt;
    private long processingTimeMs;
    
    public enum AnalysisStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED
    }
    
    public static class Finding {
        private String type;
        private String severity;
        private String file;
        private int line;
        private String description;
        private String recommendation;
        
        // Constructors, getters, and setters
        public Finding() {}
        
        public Finding(String type, String severity, String file, int line, String description, String recommendation) {
            this.type = type;
            this.severity = severity;
            this.file = file;
            this.line = line;
            this.description = description;
            this.recommendation = recommendation;
        }
        
        // Getters and setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        public String getFile() { return file; }
        public void setFile(String file) { this.file = file; }
        public int getLine() { return line; }
        public void setLine(int line) { this.line = line; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getRecommendation() { return recommendation; }
        public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
    }
    
    public static class Suggestion {
        private String category;
        private String title;
        private String description;
        private String priority;
        
        public Suggestion() {}
        
        public Suggestion(String category, String title, String description, String priority) {
            this.category = category;
            this.title = title;
            this.description = description;
            this.priority = priority;
        }
        
        // Getters and setters
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
    }
    
    // Constructors
    public CodeAnalysisResult() {
        this.completedAt = LocalDateTime.now();
    }
    
    // Getters and setters
    public String getAnalysisId() { return analysisId; }
    public void setAnalysisId(String analysisId) { this.analysisId = analysisId; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public AnalysisStatus getStatus() { return status; }
    public void setStatus(AnalysisStatus status) { this.status = status; }
    
    public String getOverallScore() { return overallScore; }
    public void setOverallScore(String overallScore) { this.overallScore = overallScore; }
    
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    
    public List<Finding> getFindings() { return findings; }
    public void setFindings(List<Finding> findings) { this.findings = findings; }
    
    public Map<String, Object> getMetrics() { return metrics; }
    public void setMetrics(Map<String, Object> metrics) { this.metrics = metrics; }
    
    public List<Suggestion> getSuggestions() { return suggestions; }
    public void setSuggestions(List<Suggestion> suggestions) { this.suggestions = suggestions; }
    
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    
    public long getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(long processingTimeMs) { this.processingTimeMs = processingTimeMs; }
}