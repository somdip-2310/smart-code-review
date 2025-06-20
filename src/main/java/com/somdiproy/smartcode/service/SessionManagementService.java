package com.somdiproy.smartcode.service;

import com.somdiproy.smartcode.model.DemoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionManagementService {
    
    private static final Logger logger = LoggerFactory.getLogger(SessionManagementService.class);
    
    private final Map<String, DemoSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<String, EmailVerification> pendingVerifications = new ConcurrentHashMap<>();
    
    @Value("${session.duration-minutes:7}")
    private int sessionDurationMinutes;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private JwtService jwtService;
    
    public EmailVerificationResult sendVerificationCode(String email) {
        try {
            String normalizedEmail = email.toLowerCase().trim();
            
            // Generate 6-digit OTP
            String code = String.format("%06d", (int)(Math.random() * 1000000));
            
            // Store verification
            pendingVerifications.put(normalizedEmail, 
                new EmailVerification(normalizedEmail, code, LocalDateTime.now()));
            
            // Send email
            boolean emailSent = emailService.sendOtpEmail(normalizedEmail, code);
            
            if (emailSent) {
                logger.info("Verification code sent to: {}", maskEmail(normalizedEmail));
                return new EmailVerificationResult(true, "Verification code sent successfully");
            } else {
                return new EmailVerificationResult(false, "Failed to send verification code");
            }
            
        } catch (Exception e) {
            logger.error("Error sending verification code", e);
            return new EmailVerificationResult(false, "System error occurred");
        }
    }
    
    public EmailVerificationResult verifyEmail(String email, String code) {
        try {
            String normalizedEmail = email.toLowerCase().trim();
            EmailVerification verification = pendingVerifications.get(normalizedEmail);
            
            if (verification == null) {
                return new EmailVerificationResult(false, "No verification code found for this email");
            }
            
            // Check expiration (5 minutes)
            if (verification.getCreatedAt().isBefore(LocalDateTime.now().minusMinutes(5))) {
                pendingVerifications.remove(normalizedEmail);
                return new EmailVerificationResult(false, "Verification code has expired");
            }
            
            if (!verification.getCode().equals(code)) {
                return new EmailVerificationResult(false, "Invalid verification code");
            }
            
            // Create session
            String sessionId = UUID.randomUUID().toString();
            DemoSession session = new DemoSession(sessionId, normalizedEmail, LocalDateTime.now());
            activeSessions.put(sessionId, session);
            
            // Remove verification
            pendingVerifications.remove(normalizedEmail);
            
            // Generate JWT
            String token = jwtService.generateToken(normalizedEmail, sessionId);
            
            logger.info("Session started for: {} with ID: {}", maskEmail(normalizedEmail), sessionId);
            
            return new EmailVerificationResult(true, "Session started successfully", sessionId, token);
            
        } catch (Exception e) {
            logger.error("Error verifying email", e);
            return new EmailVerificationResult(false, "Verification failed");
        }
    }
    
    public boolean isSessionValid(String sessionId) {
        DemoSession session = activeSessions.get(sessionId);
        if (session == null) return false;
        
        // Check if session expired
        if (session.getStartTime().isBefore(LocalDateTime.now().minusMinutes(sessionDurationMinutes))) {
            activeSessions.remove(sessionId);
            return false;
        }
        
        return true;
    }
    
    public void endSession(String sessionId) {
        activeSessions.remove(sessionId);
        logger.info("Session ended: {}", sessionId);
    }
    
    private String maskEmail(String email) {
        if (email == null || email.length() < 3) return "***";
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) return "***";
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }
    
    // Inner classes
    public static class EmailVerification {
        private final String email;
        private final String code;
        private final LocalDateTime createdAt;
        
        public EmailVerification(String email, String code, LocalDateTime createdAt) {
            this.email = email;
            this.code = code;
            this.createdAt = createdAt;
        }
        
        public String getEmail() { return email; }
        public String getCode() { return code; }
        public LocalDateTime getCreatedAt() { return createdAt; }
    }
    
    public static class EmailVerificationResult {
        private final boolean success;
        private final String message;
        private final String sessionId;
        private final String token;
        
        public EmailVerificationResult(boolean success, String message) {
            this(success, message, null, null);
        }
        
        public EmailVerificationResult(boolean success, String message, String sessionId, String token) {
            this.success = success;
            this.message = message;
            this.sessionId = sessionId;
            this.token = token;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getSessionId() { return sessionId; }
        public String getToken() { return token; }
    }
}