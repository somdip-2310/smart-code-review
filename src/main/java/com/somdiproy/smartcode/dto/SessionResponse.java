package com.somdiproy.smartcode.dto;

/**
 * Session Response DTO
 * 
 * Response object for session-related operations including:
 * - Session creation
 * - OTP verification
 * - Session status
 * 
 * @author Somdip Roy
 */
public class SessionResponse {
    
    private boolean success;
    private String sessionId;
    private String sessionToken;
    private String message;
    private Long expiresAt;
    private Integer remainingMinutes;
    private String userEmail;
    private Long createdAt;
    private SessionMetadata metadata;
    
    // Default constructor
    public SessionResponse() {
    }
    
    // Constructor with all fields
    public SessionResponse(boolean success, String sessionId, String sessionToken, String message,
                          Long expiresAt, Integer remainingMinutes, String userEmail, Long createdAt,
                          SessionMetadata metadata) {
        this.success = success;
        this.sessionId = sessionId;
        this.sessionToken = sessionToken;
        this.message = message;
        this.expiresAt = expiresAt;
        this.remainingMinutes = remainingMinutes;
        this.userEmail = userEmail;
        this.createdAt = createdAt;
        this.metadata = metadata;
    }
    
    // Builder pattern implementation
    public static SessionResponseBuilder builder() {
        return new SessionResponseBuilder();
    }
    
    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getSessionToken() {
        return sessionToken;
    }
    
    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public Long getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(Long expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public Integer getRemainingMinutes() {
        return remainingMinutes;
    }
    
    public void setRemainingMinutes(Integer remainingMinutes) {
        this.remainingMinutes = remainingMinutes;
    }
    
    public String getUserEmail() {
        return userEmail;
    }
    
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }
    
    public Long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }
    
    public SessionMetadata getMetadata() {
        return metadata;
    }
    
    public void setMetadata(SessionMetadata metadata) {
        this.metadata = metadata;
    }
    
    /**
     * Session Metadata inner class
     */
    public static class SessionMetadata {
        private String ipAddress;
        private String userAgent;
        private String location;
        private boolean demoSession;
        private int maxAnalysisCount;
        
        // Default constructor
        public SessionMetadata() {
        }
        
        // Constructor with all fields
        public SessionMetadata(String ipAddress, String userAgent, String location, 
                             boolean demoSession, int maxAnalysisCount) {
            this.ipAddress = ipAddress;
            this.userAgent = userAgent;
            this.location = location;
            this.demoSession = demoSession;
            this.maxAnalysisCount = maxAnalysisCount;
        }
        
        // Builder for SessionMetadata
        public static SessionMetadataBuilder builder() {
            return new SessionMetadataBuilder();
        }
        
        // Getters and Setters
        public String getIpAddress() {
            return ipAddress;
        }
        
        public void setIpAddress(String ipAddress) {
            this.ipAddress = ipAddress;
        }
        
        public String getUserAgent() {
            return userAgent;
        }
        
        public void setUserAgent(String userAgent) {
            this.userAgent = userAgent;
        }
        
        public String getLocation() {
            return location;
        }
        
        public void setLocation(String location) {
            this.location = location;
        }
        
        public boolean isDemoSession() {
            return demoSession;
        }
        
        public void setDemoSession(boolean demoSession) {
            this.demoSession = demoSession;
        }
        
        public int getMaxAnalysisCount() {
            return maxAnalysisCount;
        }
        
        public void setMaxAnalysisCount(int maxAnalysisCount) {
            this.maxAnalysisCount = maxAnalysisCount;
        }
        
        // Builder class for SessionMetadata
        public static class SessionMetadataBuilder {
            private String ipAddress;
            private String userAgent;
            private String location;
            private boolean demoSession;
            private int maxAnalysisCount;
            
            public SessionMetadataBuilder ipAddress(String ipAddress) {
                this.ipAddress = ipAddress;
                return this;
            }
            
            public SessionMetadataBuilder userAgent(String userAgent) {
                this.userAgent = userAgent;
                return this;
            }
            
            public SessionMetadataBuilder location(String location) {
                this.location = location;
                return this;
            }
            
            public SessionMetadataBuilder demoSession(boolean demoSession) {
                this.demoSession = demoSession;
                return this;
            }
            
            public SessionMetadataBuilder maxAnalysisCount(int maxAnalysisCount) {
                this.maxAnalysisCount = maxAnalysisCount;
                return this;
            }
            
            public SessionMetadata build() {
                return new SessionMetadata(ipAddress, userAgent, location, demoSession, maxAnalysisCount);
            }
        }
    }
    
    // Builder class for SessionResponse
    public static class SessionResponseBuilder {
        private boolean success;
        private String sessionId;
        private String sessionToken;
        private String message;
        private Long expiresAt;
        private Integer remainingMinutes;
        private String userEmail;
        private Long createdAt;
        private SessionMetadata metadata;
        
        public SessionResponseBuilder success(boolean success) {
            this.success = success;
            return this;
        }
        
        public SessionResponseBuilder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }
        
        public SessionResponseBuilder sessionToken(String sessionToken) {
            this.sessionToken = sessionToken;
            return this;
        }
        
        public SessionResponseBuilder message(String message) {
            this.message = message;
            return this;
        }
        
        public SessionResponseBuilder expiresAt(Long expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }
        
        public SessionResponseBuilder remainingMinutes(Integer remainingMinutes) {
            this.remainingMinutes = remainingMinutes;
            return this;
        }
        
        public SessionResponseBuilder userEmail(String userEmail) {
            this.userEmail = userEmail;
            return this;
        }
        
        public SessionResponseBuilder createdAt(Long createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        public SessionResponseBuilder metadata(SessionMetadata metadata) {
            this.metadata = metadata;
            return this;
        }
        
        public SessionResponse build() {
            return new SessionResponse(success, sessionId, sessionToken, message, expiresAt,
                                     remainingMinutes, userEmail, createdAt, metadata);
        }
    }
}