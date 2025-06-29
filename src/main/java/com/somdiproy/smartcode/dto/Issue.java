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
    
    // NEW FIELDS FOR DETAILED FIX INSTRUCTIONS
    private String fixInstructions;    // Step-by-step instructions
    private String searchPattern;       // What to search for
    private String replacePattern;      // What to replace with
    private String correctedCode;       // Example of fixed code
    private String implementationGuide; // Detailed implementation guide
    private String estimatedEffort;     // Time/complexity estimate
    private Double cveScore;            // CVE score if applicable
    
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
    
    // NEW GETTERS AND SETTERS
    public String getFixInstructions() {
        return fixInstructions;
    }
    
    public void setFixInstructions(String fixInstructions) {
        this.fixInstructions = fixInstructions;
    }
    
    public String getSearchPattern() {
        return searchPattern;
    }
    
    public void setSearchPattern(String searchPattern) {
        this.searchPattern = searchPattern;
    }
    
    public String getReplacePattern() {
        return replacePattern;
    }
    
    public void setReplacePattern(String replacePattern) {
        this.replacePattern = replacePattern;
    }
    
    public String getCorrectedCode() {
        return correctedCode;
    }
    
    public void setCorrectedCode(String correctedCode) {
        this.correctedCode = correctedCode;
    }
    
    public String getImplementationGuide() {
        return implementationGuide;
    }
    
    public void setImplementationGuide(String implementationGuide) {
        this.implementationGuide = implementationGuide;
    }
    
    public String getEstimatedEffort() {
        return estimatedEffort;
    }
    
    public void setEstimatedEffort(String estimatedEffort) {
        this.estimatedEffort = estimatedEffort;
    }
    
    public Double getCveScore() {
        return cveScore;
    }
    
    public void setCveScore(Double cveScore) {
        this.cveScore = cveScore;
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
        private String fixInstructions;
        private String searchPattern;
        private String replacePattern;
        private String correctedCode;
        private String implementationGuide;
        private String estimatedEffort;
        private Double cveScore;
        
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
        
        public IssueBuilder fixInstructions(String fixInstructions) {
            this.fixInstructions = fixInstructions;
            return this;
        }
        
        public IssueBuilder searchPattern(String searchPattern) {
            this.searchPattern = searchPattern;
            return this;
        }
        
        public IssueBuilder replacePattern(String replacePattern) {
            this.replacePattern = replacePattern;
            return this;
        }
        
        public IssueBuilder correctedCode(String correctedCode) {
            this.correctedCode = correctedCode;
            return this;
        }
        
        public IssueBuilder implementationGuide(String implementationGuide) {
            this.implementationGuide = implementationGuide;
            return this;
        }
        
        public IssueBuilder estimatedEffort(String estimatedEffort) {
            this.estimatedEffort = estimatedEffort;
            return this;
        }
        
        public IssueBuilder cveScore(Double cveScore) {
            this.cveScore = cveScore;
            return this;
        }
        
        public Issue build() {
            Issue issue = new Issue(id, type, severity, title, description, fileName, 
                           lineNumber, codeSnippet, suggestion, category);
            issue.setFixInstructions(fixInstructions);
            issue.setSearchPattern(searchPattern);
            issue.setReplacePattern(replacePattern);
            issue.setCorrectedCode(correctedCode);
            issue.setImplementationGuide(implementationGuide);
            issue.setEstimatedEffort(estimatedEffort);
            issue.setCveScore(cveScore);
            return issue;
        }
    }
}