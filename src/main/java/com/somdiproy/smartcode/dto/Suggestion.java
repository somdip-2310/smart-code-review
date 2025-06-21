package com.somdiproy.smartcode.dto;

/**
 * Code Improvement Suggestion DTO
 * Represents recommendations for code improvements
 * 
 * @author Somdip Roy
 */
public class Suggestion {
    
    private String id;
    private String title;
    private String description;
    private String category;        // Refactoring, Performance, Security, Best Practice
    private String impact;          // HIGH, MEDIUM, LOW
    private String implementation;   // Detailed steps to implement
    private String example;         // Code example
    private int estimatedEffort;    // In minutes
    private String priority;        // P0, P1, P2, P3
    
    // Default constructor
    public Suggestion() {
    }
    
    // Constructor with essential fields
    public Suggestion(String title, String description, String category, String impact) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.impact = impact;
    }
    
    // Builder pattern
    public static SuggestionBuilder builder() {
        return new SuggestionBuilder();
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
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
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public String getImpact() {
        return impact;
    }
    
    public void setImpact(String impact) {
        this.impact = impact;
    }
    
    public String getImplementation() {
        return implementation;
    }
    
    public void setImplementation(String implementation) {
        this.implementation = implementation;
    }
    
    public String getExample() {
        return example;
    }
    
    public void setExample(String example) {
        this.example = example;
    }
    
    public int getEstimatedEffort() {
        return estimatedEffort;
    }
    
    public void setEstimatedEffort(int estimatedEffort) {
        this.estimatedEffort = estimatedEffort;
    }
    
    public String getPriority() {
        return priority;
    }
    
    public void setPriority(String priority) {
        this.priority = priority;
    }
    
    // Builder class
    public static class SuggestionBuilder {
        private String id;
        private String title;
        private String description;
        private String category;
        private String impact;
        private String implementation;
        private String example;
        private int estimatedEffort;
        private String priority;
        
        public SuggestionBuilder id(String id) {
            this.id = id;
            return this;
        }
        
        public SuggestionBuilder title(String title) {
            this.title = title;
            return this;
        }
        
        public SuggestionBuilder description(String description) {
            this.description = description;
            return this;
        }
        
        public SuggestionBuilder category(String category) {
            this.category = category;
            return this;
        }
        
        public SuggestionBuilder impact(String impact) {
            this.impact = impact;
            return this;
        }
        
        public SuggestionBuilder implementation(String implementation) {
            this.implementation = implementation;
            return this;
        }
        
        public SuggestionBuilder example(String example) {
            this.example = example;
            return this;
        }
        
        public SuggestionBuilder estimatedEffort(int estimatedEffort) {
            this.estimatedEffort = estimatedEffort;
            return this;
        }
        
        public SuggestionBuilder priority(String priority) {
            this.priority = priority;
            return this;
        }
        
        public Suggestion build() {
            Suggestion suggestion = new Suggestion();
            suggestion.id = this.id;
            suggestion.title = this.title;
            suggestion.description = this.description;
            suggestion.category = this.category;
            suggestion.impact = this.impact;
            suggestion.implementation = this.implementation;
            suggestion.example = this.example;
            suggestion.estimatedEffort = this.estimatedEffort;
            suggestion.priority = this.priority;
            return suggestion;
        }
    }
}