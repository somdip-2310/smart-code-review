package com.somdiproy.smartcode.dto;

/**
 * Analysis Request DTO
 * 
 * @author Somdip Roy
 */
public class AnalysisRequest {
    
    private AnalysisType type;
    private String sessionToken;
    private String fileName;
    private Long fileSize;
    private String language;
    private String clientIp;
    private String userAgent;
    
    // Default constructor
    public AnalysisRequest() {
    }
    
    // Builder pattern implementation
    public static AnalysisRequestBuilder builder() {
        return new AnalysisRequestBuilder();
    }
    
    // Getters and Setters
    public AnalysisType getType() {
        return type;
    }
    
    public void setType(AnalysisType type) {
        this.type = type;
    }
    
    public String getSessionToken() {
        return sessionToken;
    }
    
    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public Long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }
    
    public String getLanguage() {
        return language;
    }
    
    public void setLanguage(String language) {
        this.language = language;
    }
    
    public String getClientIp() {
        return clientIp;
    }
    
    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }
    
    public String getUserAgent() {
        return userAgent;
    }
    
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    
    // Builder class
    public static class AnalysisRequestBuilder {
        private AnalysisType type;
        private String sessionToken;
        private String fileName;
        private Long fileSize;
        private String language;
        private String clientIp;
        private String userAgent;
        
        public AnalysisRequestBuilder type(AnalysisType type) {
            this.type = type;
            return this;
        }
        
        public AnalysisRequestBuilder sessionToken(String sessionToken) {
            this.sessionToken = sessionToken;
            return this;
        }
        
        public AnalysisRequestBuilder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }
        
        public AnalysisRequestBuilder fileSize(Long fileSize) {
            this.fileSize = fileSize;
            return this;
        }
        
        public AnalysisRequestBuilder language(String language) {
            this.language = language;
            return this;
        }
        
        public AnalysisRequestBuilder clientIp(String clientIp) {
            this.clientIp = clientIp;
            return this;
        }
        
        public AnalysisRequestBuilder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }
        
        public AnalysisRequest build() {
            AnalysisRequest request = new AnalysisRequest();
            request.type = this.type;
            request.sessionToken = this.sessionToken;
            request.fileName = this.fileName;
            request.fileSize = this.fileSize;
            request.language = this.language;
            request.clientIp = this.clientIp;
            request.userAgent = this.userAgent;
            return request;
        }
    }
}