package com.somdiproy.smartcode.dto;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Code Issue DTO
 * Represents a code issue found during analysis
 * 
 * @author Somdip Roy
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Issue {
    
    private String id;
    private String type;        // BUG, CODE_SMELL, VULNERABILITY, SECURITY_HOTSPOT
    private String severity;    // CRITICAL, HIGH, MEDIUM, LOW, INFO
    private String title;
    private String description;
    private String fileName;
    
    @JsonProperty("lineNumber")
    @JsonAlias({"line", "lineNumber"})
    private int lineNumber;
    
    private String codeSnippet;
    private String suggestion;
    private String category;     // Security, Performance, Quality, Best Practice
    
    // Default constructor
    public Issue() {
    }
    
    // Constructor with all fields
    public Issue(String id, String type, String severity, String title, String description,
                 String fileName, int lineNumber, String codeSnippet, String suggestion, String category) {
        this.id = id;
        this.type = type;
        this.severity = severity;
        this.title = title;
        this.description = description;
        this.fileName = fileName;
        this.lineNumber = lineNumber;
        this.codeSnippet = codeSnippet;
        this.suggestion = suggestion;
        this.category = category;
    }
    
    // Builder pattern
    public static IssueBuilder builder() {
        return new IssueBuilder();
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
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
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public int getLineNumber() {
        return lineNumber;
    }
    
    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }
    
    public String getCodeSnippet() {
        return codeSnippet;
    }
    
    public void setCodeSnippet(String codeSnippet) {
        this.codeSnippet = codeSnippet;
    }
    
    public String getSuggestion() {
        return suggestion;
    }
    
    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    // Builder class
    public static class IssueBuilder {
        private String id;
        private String type;
        private String severity;
        private String title;
        private String description;
        private String fileName;
        private int lineNumber;
        private String codeSnippet;
        private String suggestion;
        private String category;
        
        public IssueBuilder id(String id) {
            this.id = id;
            return this;
        }
        
        public IssueBuilder type(String type) {
            this.type = type;
            return this;
        }
        
        public IssueBuilder severity(String severity) {
            this.severity = severity;
            return this;
        }
        
        public IssueBuilder title(String title) {
            this.title = title;
            return this;
        }
        
        public IssueBuilder description(String description) {
            this.description = description;
            return this;
        }
        
        public IssueBuilder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }
        
        public IssueBuilder lineNumber(int lineNumber) {
            this.lineNumber = lineNumber;
            return this;
        }
        
        public IssueBuilder codeSnippet(String codeSnippet) {
            this.codeSnippet = codeSnippet;
            return this;
        }
        
        public IssueBuilder suggestion(String suggestion) {
            this.suggestion = suggestion;
            return this;
        }
        
        public IssueBuilder category(String category) {
            this.category = category;
            return this;
        }
        
        public Issue build() {
            return new Issue(id, type, severity, title, description, fileName, 
                           lineNumber, codeSnippet, suggestion, category);
        }
    }
}