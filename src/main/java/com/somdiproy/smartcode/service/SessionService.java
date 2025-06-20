package com.somdiproy.smartcode.service;

import com.somdiproy.smartcode.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Session Management Service for Smart Code Review
 * 
 * Handles demo session creation, OTP verification, and session lifecycle
 * management for the Smart Code Review platform.
 * 
 * Features:
 * - 7-minute demo sessions
 * - Email-based OTP verification
 * - Session token management
 * - Automatic cleanup of expired sessions
 * 
 * @author Somdip Roy
 * @version 1.0.0
 */
@Service
public class SessionService {
    
    private static final Logger logger = LoggerFactory.getLogger(SessionService.class);
    
    @Value("${demo.session.duration.minutes:7}")
    private int sessionDurationMinutes;
    
    @Value("${demo.session.cleanup.interval:300000}")
    private long cleanupIntervalMs;
    
    @Autowired
    private EmailService emailService;
    
    // In-memory storage for demo sessions (use Redis in production)
    private final Map<String, SessionData> activeSessions = new ConcurrentHashMap<>();
    private final Map<String, SessionData> pendingSessions = new ConcurrentHashMap<>();
    
    /**
     * Create a new demo session
     */
    public SessionResponse createSession(SessionRequest request) {
        try {
            // Generate session ID and OTP
            String sessionId = generateSessionId();
            String otp = generateOtp();
            
            // Create session data
            SessionData sessionData = SessionData.builder()
                    .sessionId(sessionId)
                    .email(request.getEmail())
                    .name(request.getName())
                    .otp(otp)
                    .createdAt(System.currentTimeMillis())
                    .verified(false)
                    .build();
            
            // Store in pending sessions
            pendingSessions.put(sessionId, sessionData);
            
            // Send OTP email
            emailService.sendOtpEmail(request.getEmail(), request.getName(), otp);
            
            logger.info("Session created: {} for email: {}", sessionId, maskEmail(request.getEmail()));
            
            return SessionResponse.builder()
                    .success(true)
                    .message("Session created successfully. Please check your email for the verification code.")
                    .sessionId(sessionId)
                    .build();
                    
        } catch (Exception e) {
            logger.error("Error creating session", e);
            return SessionResponse.builder()
                    .success(false)
                    .message("Failed to create session: " + e.getMessage())
                    .build();
        }
    }
    
    /**
     * Verify OTP and activate session
     */
    public SessionResponse verifyOtp(OtpVerificationRequest request) {
        try {
            SessionData sessionData = pendingSessions.get(request.getSessionId());
            
            if (sessionData == null) {
                return SessionResponse.builder()
                        .success(false)
                        .message("Session not found or expired")
                        .build();
            }
            
            // Check if OTP is expired (10 minutes)
            long otpAge = System.currentTimeMillis() - sessionData.getCreatedAt();
            if (otpAge > 600000) { // 10 minutes
                pendingSessions.remove(request.getSessionId());
                return SessionResponse.builder()
                        .success(false)
                        .message("OTP has expired. Please request a new session.")
                        .build();
            }
            
            // Verify OTP
            if (!sessionData.getOtp().equals(request.getOtp())) {
                return SessionResponse.builder()
                        .success(false)
                        .message("Invalid verification code")
                        .build();
            }
            
            // Generate session token
            String sessionToken = generateSessionToken();
            sessionData.setSessionToken(sessionToken);
            sessionData.setVerified(true);
            sessionData.setCreatedAt(System.currentTimeMillis()); // Reset timer for active session
            
            // Move to active sessions
            activeSessions.put(request.getSessionId(), sessionData);
            pendingSessions.remove(request.getSessionId());
            
            logger.info("Session verified: {} for email: {}", request.getSessionId(), maskEmail(sessionData.getEmail()));
            
            return SessionResponse.builder()
                    .success(true)
                    .message("Session verified successfully")
                    .sessionId(request.getSessionId())
                    .sessionToken(sessionToken)
                    .expiresAt(System.currentTimeMillis() + (sessionDurationMinutes * 60 * 1000))
                    .build();
                    
        } catch (Exception e) {
            logger.error("Error verifying OTP", e);
            return SessionResponse.builder()
                    .success(false)
                    .message("Failed to verify session: " + e.getMessage())
                    .build();
        }
    }
    
    /**
     * Validate session token for API requests
     */
    public boolean isValidSession(String sessionToken) {
        if (sessionToken == null || sessionToken.trim().isEmpty()) {
            return false;
        }
        
        // Cleanup expired sessions first
        cleanupExpiredSessions();
        
        return activeSessions.values().stream()
                .anyMatch(session -> sessionToken.equals(session.getSessionToken()) && 
                         session.isVerified() && 
                         !isSessionExpired(session));
    }
    
    /**
     * Get session information
     */
    public SessionData getSessionByToken(String sessionToken) {
        return activeSessions.values().stream()
                .filter(session -> sessionToken.equals(session.getSessionToken()))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Get current session status
     */
    public Map<String, Object> getSessionStatus() {
        cleanupExpiredSessions();
        
        Map<String, Object> status = new ConcurrentHashMap<>();
        status.put("activeSessions", activeSessions.size());
        status.put("pendingSessions", pendingSessions.size());
        status.put("sessionDurationMinutes", sessionDurationMinutes);
        status.put("timestamp", System.currentTimeMillis());
        
        return status;
    }
    
    /**
     * Cleanup expired sessions
     */
    public void cleanupExpiredSessions() {
        long currentTime = System.currentTimeMillis();
        long sessionDurationMs = sessionDurationMinutes * 60 * 1000;
        long otpExpirationMs = 600000; // 10 minutes
        
        // Clean expired active sessions
        activeSessions.entrySet().removeIf(entry -> {
            SessionData session = entry.getValue();
            boolean expired = (currentTime - session.getCreatedAt()) > sessionDurationMs;
            if (expired) {
                logger.info("Cleaned up expired session: {} for email: {}", 
                           entry.getKey(), maskEmail(session.getEmail()));
            }
            return expired;
        });
        
        // Clean expired pending sessions
        pendingSessions.entrySet().removeIf(entry -> {
            SessionData session = entry.getValue();
            boolean expired = (currentTime - session.getCreatedAt()) > otpExpirationMs;
            if (expired) {
                logger.debug("Cleaned up expired pending session: {}", entry.getKey());
            }
            return expired;
        });
    }
    
    /**
     * Force end a session
     */
    public boolean endSession(String sessionToken) {
        SessionData session = getSessionByToken(sessionToken);
        if (session != null) {
            activeSessions.entrySet().removeIf(entry -> 
                sessionToken.equals(entry.getValue().getSessionToken()));
            logger.info("Session ended: {} for email: {}", 
                       session.getSessionId(), maskEmail(session.getEmail()));
            return true;
        }
        return false;
    }
    
    // Private helper methods
    
    private String generateSessionId() {
        return "session_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
    
    private String generateSessionToken() {
        return "token_" + UUID.randomUUID().toString().replace("-", "");
    }
    
    private String generateOtp() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }
    
    private boolean isSessionExpired(SessionData session) {
        long sessionAge = System.currentTimeMillis() - session.getCreatedAt();
        return sessionAge > (sessionDurationMinutes * 60 * 1000);
    }
    
    private String maskEmail(String email) {
        if (email == null || email.length() < 3) return "***";
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) return "***";
        return email.substring(0, 1) + "***" + email.substring(atIndex);
    }
    
    /**
     * Session Data inner class
     */
    public static class SessionData {
        private String sessionId;
        private String email;
        private String name;
        private String otp;
        private String sessionToken;
        private long createdAt;
        private boolean verified;
        
        // Constructors
        public SessionData() {}
        
        public static SessionDataBuilder builder() {
            return new SessionDataBuilder();
        }
        
        // Getters and Setters
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getOtp() { return otp; }
        public void setOtp(String otp) { this.otp = otp; }
        
        public String getSessionToken() { return sessionToken; }
        public void setSessionToken(String sessionToken) { this.sessionToken = sessionToken; }
        
        public long getCreatedAt() { return createdAt; }
        public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
        
        public boolean isVerified() { return verified; }
        public void setVerified(boolean verified) { this.verified = verified; }
        
        // Builder class
        public static class SessionDataBuilder {
            private String sessionId;
            private String email;
            private String name;
            private String otp;
            private long createdAt;
            private boolean verified;
            
            public SessionDataBuilder sessionId(String sessionId) {
                this.sessionId = sessionId;
                return this;
            }
            
            public SessionDataBuilder email(String email) {
                this.email = email;
                return this;
            }
            
            public SessionDataBuilder name(String name) {
                this.name = name;
                return this;
            }
            
            public SessionDataBuilder otp(String otp) {
                this.otp = otp;
                return this;
            }
            
            public SessionDataBuilder createdAt(long createdAt) {
                this.createdAt = createdAt;
                return this;
            }
            
            public SessionDataBuilder verified(boolean verified) {
                this.verified = verified;
                return this;
            }
            
            public SessionData build() {
                SessionData data = new SessionData();
                data.sessionId = this.sessionId;
                data.email = this.email;
                data.name = this.name;
                data.otp = this.otp;
                data.createdAt = this.createdAt;
                data.verified = this.verified;
                return data;
            }
        }
    }
}