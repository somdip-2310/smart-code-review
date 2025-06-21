package com.somdiproy.smartcode.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Security Analysis Result DTO
 * Contains detailed security assessment of the code
 * 
 * @author Somdip Roy
 */
public class SecurityAnalysis {
    
    private double securityScore;           // 0-10 scale
    private List<String> vulnerabilities;   // List of vulnerability types found
    private List<String> recommendations;   // Security recommendations
    private boolean hasSecurityIssues;
    private int criticalIssuesCount;
    private int highIssuesCount;
    private int mediumIssuesCount;
    private int lowIssuesCount;
    private List<SecurityVulnerability> detailedVulnerabilities;
    private String overallAssessment;
    
    // Default constructor
    public SecurityAnalysis() {
        this.vulnerabilities = new ArrayList<>();
        this.recommendations = new ArrayList<>();
        this.detailedVulnerabilities = new ArrayList<>();
    }
    
    // Builder pattern
    public static SecurityAnalysisBuilder builder() {
        return new SecurityAnalysisBuilder();
    }
    
    // Getters and Setters
    public double getSecurityScore() {
        return securityScore;
    }
    
    public void setSecurityScore(double securityScore) {
        this.securityScore = securityScore;
    }
    
    public List<String> getVulnerabilities() {
        return vulnerabilities;
    }
    
    public void setVulnerabilities(List<String> vulnerabilities) {
        this.vulnerabilities = vulnerabilities;
    }
    
    public List<String> getRecommendations() {
        return recommendations;
    }
    
    public void setRecommendations(List<String> recommendations) {
        this.recommendations = recommendations;
    }
    
    public boolean isHasSecurityIssues() {
        return hasSecurityIssues;
    }
    
    public void setHasSecurityIssues(boolean hasSecurityIssues) {
        this.hasSecurityIssues = hasSecurityIssues;
    }
    
    public int getCriticalIssuesCount() {
        return criticalIssuesCount;
    }
    
    public void setCriticalIssuesCount(int criticalIssuesCount) {
        this.criticalIssuesCount = criticalIssuesCount;
    }
    
    public int getHighIssuesCount() {
        return highIssuesCount;
    }
    
    public void setHighIssuesCount(int highIssuesCount) {
        this.highIssuesCount = highIssuesCount;
    }
    
    public int getMediumIssuesCount() {
        return mediumIssuesCount;
    }
    
    public void setMediumIssuesCount(int mediumIssuesCount) {
        this.mediumIssuesCount = mediumIssuesCount;
    }
    
    public int getLowIssuesCount() {
        return lowIssuesCount;
    }
    
    public void setLowIssuesCount(int lowIssuesCount) {
        this.lowIssuesCount = lowIssuesCount;
    }
    
    public List<SecurityVulnerability> getDetailedVulnerabilities() {
        return detailedVulnerabilities;
    }
    
    public void setDetailedVulnerabilities(List<SecurityVulnerability> detailedVulnerabilities) {
        this.detailedVulnerabilities = detailedVulnerabilities;
    }
    
    public String getOverallAssessment() {
        return overallAssessment;
    }
    
    public void setOverallAssessment(String overallAssessment) {
        this.overallAssessment = overallAssessment;
    }
    
    // Inner class for detailed vulnerability information
    public static class SecurityVulnerability {
        private String type;        // SQL Injection, XSS, etc.
        private String severity;    // CRITICAL, HIGH, MEDIUM, LOW
        private String description;
        private String location;    // File and line number
        private String remediation;
        
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
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public String getLocation() {
            return location;
        }
        
        public void setLocation(String location) {
            this.location = location;
        }
        
        public String getRemediation() {
            return remediation;
        }
        
        public void setRemediation(String remediation) {
            this.remediation = remediation;
        }
    }
    
    // Builder class
    public static class SecurityAnalysisBuilder {
        private double securityScore;
        private List<String> vulnerabilities = new ArrayList<>();
        private List<String> recommendations = new ArrayList<>();
        private boolean hasSecurityIssues;
        private int criticalIssuesCount;
        private int highIssuesCount;
        private int mediumIssuesCount;
        private int lowIssuesCount;
        private List<SecurityVulnerability> detailedVulnerabilities = new ArrayList<>();
        private String overallAssessment;
        
        public SecurityAnalysisBuilder securityScore(double securityScore) {
            this.securityScore = securityScore;
            return this;
        }
        
        public SecurityAnalysisBuilder vulnerabilities(List<String> vulnerabilities) {
            this.vulnerabilities = vulnerabilities;
            return this;
        }
        
        public SecurityAnalysisBuilder recommendations(List<String> recommendations) {
            this.recommendations = recommendations;
            return this;
        }
        
        public SecurityAnalysisBuilder hasSecurityIssues(boolean hasSecurityIssues) {
            this.hasSecurityIssues = hasSecurityIssues;
            return this;
        }
        
        public SecurityAnalysisBuilder criticalIssuesCount(int criticalIssuesCount) {
            this.criticalIssuesCount = criticalIssuesCount;
            return this;
        }
        
        public SecurityAnalysisBuilder highIssuesCount(int highIssuesCount) {
            this.highIssuesCount = highIssuesCount;
            return this;
        }
        
        public SecurityAnalysisBuilder mediumIssuesCount(int mediumIssuesCount) {
            this.mediumIssuesCount = mediumIssuesCount;
            return this;
        }
        
        public SecurityAnalysisBuilder lowIssuesCount(int lowIssuesCount) {
            this.lowIssuesCount = lowIssuesCount;
            return this;
        }
        
        public SecurityAnalysisBuilder detailedVulnerabilities(List<SecurityVulnerability> detailedVulnerabilities) {
            this.detailedVulnerabilities = detailedVulnerabilities;
            return this;
        }
        
        public SecurityAnalysisBuilder overallAssessment(String overallAssessment) {
            this.overallAssessment = overallAssessment;
            return this;
        }
        
        public SecurityAnalysis build() {
            SecurityAnalysis analysis = new SecurityAnalysis();
            analysis.securityScore = this.securityScore;
            analysis.vulnerabilities = this.vulnerabilities;
            analysis.recommendations = this.recommendations;
            analysis.hasSecurityIssues = this.hasSecurityIssues;
            analysis.criticalIssuesCount = this.criticalIssuesCount;
            analysis.highIssuesCount = this.highIssuesCount;
            analysis.mediumIssuesCount = this.mediumIssuesCount;
            analysis.lowIssuesCount = this.lowIssuesCount;
            analysis.detailedVulnerabilities = this.detailedVulnerabilities;
            analysis.overallAssessment = this.overallAssessment;
            return analysis;
        }
    }
}