package com.somdiproy.smartcode.service;

import com.somdiproy.smartcode.dto.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
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
    
    @Value("${demo.session.max.analysis:5}")
    private int maxAnalysisPerSession;
    
    @Autowired
    private EmailService emailService;
    
    // In-memory storage for demo sessions (use Redis in production)
    private final Map<String, SessionData> activeSessions = new ConcurrentHashMap<>();
    private final Map<String, SessionData> pendingSessions = new ConcurrentHashMap<>();
    private final Map<String, String> sessionTokenToId = new ConcurrentHashMap<>();
    
    /**
     * Create a new demo session
     */
    public SessionResponse createSession(SessionRequest request) {
        try {
            // Validate email
            if (request.getEmail() == null || !isValidEmail(request.getEmail())) {
                return SessionResponse.builder()
                        .success(false)
                        .message("Invalid email address")
                        .build();
            }
            
            // Check if user already has an active session
            SessionData existingSession = findActiveSessionByEmail(request.getEmail());
            if (existingSession != null) {
                return SessionResponse.builder()
                        .success(false)
                        .message("You already have an active session. Please use the existing session or wait for it to expire.")
                        .sessionId(existingSession.getSessionId())
                        .build();
            }
            
            // Generate session ID and OTP
            String sessionId = generateSessionId();
            String otp = generateOtp();
            
            // Create session data
            SessionData sessionData = SessionData.builder()
                    .sessionId(sessionId)
                    .email(request.getEmail())
                    .name(request.getName())
                    .company(request.getCompany())
                    .otp(otp)
                    .createdAt(System.currentTimeMillis())
                    .verified(false)
                    .analysisCount(0)
                    .otpAttempts(0)
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
                    .createdAt(System.currentTimeMillis())
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
                sessionData.setOtpAttempts(sessionData.getOtpAttempts() + 1);
                
                // Lock out after 3 failed attempts
                if (sessionData.getOtpAttempts() >= 3) {
                    pendingSessions.remove(request.getSessionId());
                    return SessionResponse.builder()
                            .success(false)
                            .message("Too many failed attempts. Please request a new session.")
                            .build();
                }
                
                return SessionResponse.builder()
                        .success(false)
                        .message("Invalid verification code. " + (3 - sessionData.getOtpAttempts()) + " attempts remaining.")
                        .build();
            }
            
            // Generate session token
            String sessionToken = generateSessionToken();
            sessionData.setSessionToken(sessionToken);
            sessionData.setVerified(true);
            sessionData.setCreatedAt(System.currentTimeMillis()); // Reset timer for active session
            
            // Move to active sessions
            activeSessions.put(request.getSessionId(), sessionData);
            sessionTokenToId.put(sessionToken, request.getSessionId());
            pendingSessions.remove(request.getSessionId());
            
            long expiresAt = sessionData.getCreatedAt() + (sessionDurationMinutes * 60 * 1000);
            
            logger.info("Session verified: {} for email: {}", request.getSessionId(), maskEmail(sessionData.getEmail()));
            
            return SessionResponse.builder()
                    .success(true)
                    .message("Session verified successfully")
                    .sessionId(request.getSessionId())
                    .sessionToken(sessionToken)
                    .expiresAt(expiresAt)
                    .remainingMinutes(sessionDurationMinutes)
                    .userEmail(maskEmail(sessionData.getEmail()))
                    .metadata(SessionResponse.SessionMetadata.builder()
                            .demoSession(true)
                            .maxAnalysisCount(maxAnalysisPerSession)
                            .build())
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
        
        String sessionId = sessionTokenToId.get(sessionToken);
        if (sessionId == null) {
            return false;
        }
        
        SessionData sessionData = activeSessions.get(sessionId);
        if (sessionData == null || !sessionData.isVerified()) {
            return false;
        }
        
        if (isSessionExpired(sessionData)) {
            endSession(sessionToken);
            return false;
        }
        
        return true;
    }
    
    /**
     * Get session information by token
     */
    public SessionData getSessionByToken(String sessionToken) {
        String sessionId = sessionTokenToId.get(sessionToken);
        if (sessionId == null) {
            return null;
        }
        
        return activeSessions.get(sessionId);
    }
    
    /**
     * Get remaining session time in minutes
     */
    public int getRemainingTime(String sessionToken) {
        SessionData sessionData = getSessionByToken(sessionToken);
        if (sessionData == null) {
            return 0;
        }
        
        return calculateRemainingMinutes(sessionData);
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
    @Scheduled(fixedDelayString = "${demo.session.cleanup.interval:300000}") // 5 minutes
    public void cleanupExpiredSessions() {
        long currentTime = System.currentTimeMillis();
        long sessionDurationMs = sessionDurationMinutes * 60 * 1000;
        long otpExpirationMs = 600000; // 10 minutes
        
        // Clean expired active sessions
        activeSessions.entrySet().removeIf(entry -> {
            SessionData session = entry.getValue();
            boolean expired = (currentTime - session.getCreatedAt()) > sessionDurationMs;
            if (expired) {
                sessionTokenToId.remove(session.getSessionToken());
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
        String sessionId = sessionTokenToId.get(sessionToken);
        if (sessionId == null) {
            return false;
        }
        
        SessionData sessionData = activeSessions.remove(sessionId);
        sessionTokenToId.remove(sessionToken);
        
        if (sessionData != null) {
            logger.info("Session ended: {} for email: {}", sessionId, maskEmail(sessionData.getEmail()));
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
        SecureRandom random = new SecureRandom();
        return String.format("%06d", random.nextInt(1000000));
    }
    
    private boolean isSessionExpired(SessionData session) {
        long sessionAge = System.currentTimeMillis() - session.getCreatedAt();
        return sessionAge > (sessionDurationMinutes * 60 * 1000);
    }
    
    private int calculateRemainingMinutes(SessionData sessionData) {
        long currentTime = System.currentTimeMillis();
        long sessionAge = currentTime - sessionData.getCreatedAt();
        long remainingTime = (sessionDurationMinutes * 60 * 1000) - sessionAge;
        return Math.max(0, (int) (remainingTime / 60000));
    }
    
    private SessionData findActiveSessionByEmail(String email) {
        return activeSessions.values().stream()
                .filter(session -> email.equalsIgnoreCase(session.getEmail()))
                .findFirst()
                .orElse(null);
    }
    
    private boolean isValidEmail(String email) {
        return email != null && 
               email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
    
    private String maskEmail(String email) {
        if (email == null || email.length() < 3) return "***";
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) return "***";
        
        String username = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        
        if (username.length() <= 2) {
            return username.charAt(0) + "*" + domain;
        } else {
            return username.charAt(0) + 
                   "*".repeat(Math.min(username.length() - 2, 3)) + 
                   username.charAt(username.length() - 1) + 
                   domain;
        }
    }
    
    /**
     * Session Data inner class with Lombok annotations
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionData {
        private String sessionId;
        private String email;
        private String name;
        private String company;
        private String otp;
        private String sessionToken;
        private long createdAt;
        private boolean verified;
        private int analysisCount;
        private int otpAttempts;
    }
}