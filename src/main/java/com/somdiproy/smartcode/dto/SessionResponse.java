package com.somdiproy.smartcode.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionResponse {
    
    /**
     * Indicates if the operation was successful
     */
    private boolean success;
    
    /**
     * Unique session identifier
     */
    private String sessionId;
    
    /**
     * Session token for authenticated requests
     */
    private String sessionToken;
    
    /**
     * Response message (success or error details)
     */
    private String message;
    
    /**
     * Session expiration timestamp (milliseconds)
     */
    private Long expiresAt;
    
    /**
     * Remaining session time in minutes
     */
    private Integer remainingMinutes;
    
    /**
     * User email (masked for privacy)
     */
    private String userEmail;
    
    /**
     * Session creation timestamp
     */
    private Long createdAt;
    
    /**
     * Additional session metadata
     */
    private SessionMetadata metadata;
    
    /**
     * Session Metadata inner class
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionMetadata {
        private String ipAddress;
        private String userAgent;
        private String location;
        private boolean demoSession;
        private int maxAnalysisCount;
    }
}