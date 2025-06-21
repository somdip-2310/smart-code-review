package com.somdiproy.smartcode.dto;

/**
 * Analysis Response DTO
 * 
 * @author Somdip Roy
 */
public class AnalysisResponse {
    
    private boolean success;
    private String analysisId;
    private AnalysisStatus status;
    private String message;
    private CodeReviewResult result;
    private long createdAt;
    private long updatedAt;
    private int progressPercentage;
    private String s3Key;  // Added s3Key field
    
    // Default constructor
    public AnalysisResponse() {
    }
    
    // Constructor with all fields
    public AnalysisResponse(boolean success, String analysisId, AnalysisStatus status, String message,
                           CodeReviewResult result, long createdAt, long updatedAt, int progressPercentage,
                           String s3Key) {
        this.success = success;
        this.analysisId = analysisId;
        this.status = status;
        this.message = message;
        this.result = result;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.progressPercentage = progressPercentage;
        this.s3Key = s3Key;
    }
    
    // Builder pattern implementation
    public static AnalysisResponseBuilder builder() {
        return new AnalysisResponseBuilder();
    }
    
    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getAnalysisId() {
        return analysisId;
    }
    
    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
    }
    
    public AnalysisStatus getStatus() {
        return status;
    }
    
    public void setStatus(AnalysisStatus status) {
        this.status = status;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public CodeReviewResult getResult() {
        return result;
    }
    
    public void setResult(CodeReviewResult result) {
        this.result = result;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    
    public long getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public int getProgressPercentage() {
        return progressPercentage;
    }
    
    public void setProgressPercentage(int progressPercentage) {
        this.progressPercentage = progressPercentage;
    }
    
    public String getS3Key() {
        return s3Key;
    }
    
    public void setS3Key(String s3Key) {
        this.s3Key = s3Key;
    }
    
    // Builder class
    public static class AnalysisResponseBuilder {
        private boolean success;
        private String analysisId;
        private AnalysisStatus status;
        private String message;
        private CodeReviewResult result;
        private long createdAt;
        private long updatedAt;
        private int progressPercentage;
        private String s3Key;
        
        public AnalysisResponseBuilder success(boolean success) {
            this.success = success;
            return this;
        }
        
        public AnalysisResponseBuilder analysisId(String analysisId) {
            this.analysisId = analysisId;
            return this;
        }
        
        public AnalysisResponseBuilder status(AnalysisStatus status) {
            this.status = status;
            return this;
        }
        
        public AnalysisResponseBuilder message(String message) {
            this.message = message;
            return this;
        }
        
        public AnalysisResponseBuilder result(CodeReviewResult result) {
            this.result = result;
            return this;
        }
        
        public AnalysisResponseBuilder createdAt(long createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        public AnalysisResponseBuilder updatedAt(long updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }
        
        public AnalysisResponseBuilder progressPercentage(int progressPercentage) {
            this.progressPercentage = progressPercentage;
            return this;
        }
        
        public AnalysisResponseBuilder s3Key(String s3Key) {
            this.s3Key = s3Key;
            return this;
        }
        
        public AnalysisResponse build() {
            AnalysisResponse response = new AnalysisResponse();
            response.success = this.success;
            response.analysisId = this.analysisId;
            response.status = this.status;
            response.message = this.message;
            response.result = this.result;
            response.createdAt = this.createdAt;
            response.updatedAt = this.updatedAt;
            response.progressPercentage = this.progressPercentage;
            response.s3Key = this.s3Key;
            return response;
        }
    }
}