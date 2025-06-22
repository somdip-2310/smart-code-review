package com.somdiproy.smartcode.service;

import com.somdiproy.smartcode.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
    
    @Value("${sendgrid.api.key:}")
    private String sendGridApiKey;
    
    @Autowired
    private EmailService emailService;
    
    // In-memory storage for demo sessions (use Redis in production)
    private final Map<String, SessionData> activeSessions = new ConcurrentHashMap<>();
    private final Map<String, SessionData> pendingSessions = new ConcurrentHashMap<>();
    private final Map<String, String> sessionTokenToId = new ConcurrentHashMap<>();
    
    @Value("${demo.session.max.concurrent:100}")
    private int maxConcurrentSessions;

    @Value("${demo.session.max.per.ip:3}")
    private int maxSessionsPerIp;

    private final Map<String, List<String>> ipToSessions = new ConcurrentHashMap<>();
    
    public SessionResponse createSession(SessionRequest request) {
        try {
            logger.info("=== SESSION CREATION STARTED ===");
            logger.info("Email: {}", request.getEmail());
            logger.info("Name: {}", request.getName());
            
            // Check global session limit
            int totalActiveSessions = activeSessions.size() + pendingSessions.size();
            if (totalActiveSessions >= maxConcurrentSessions) {
                logger.warn("Maximum concurrent sessions reached: {}", totalActiveSessions);
                return SessionResponse.builder()
                        .success(false)
                        .message("System is at capacity. Please try again in a few minutes.")
                        .build();
            }
            
            // Check IP-based limits
            String clientIp = request.getClientIp();
            if (clientIp != null && !clientIp.isEmpty()) {
                List<String> ipSessions = ipToSessions.computeIfAbsent(clientIp, k -> new ArrayList<>());
                
                // Clean up expired sessions for this IP
                ipSessions.removeIf(sessionId -> {
                    SessionData session = activeSessions.get(sessionId);
                    SessionData pendingSession = pendingSessions.get(sessionId);
                    return (session == null || isSessionExpired(session)) && 
                           (pendingSession == null || isSessionExpired(pendingSession));
                });
                
                if (ipSessions.size() >= maxSessionsPerIp) {
                    logger.warn("Too many sessions from IP: {}", clientIp);
                    return SessionResponse.builder()
                            .success(false)
                            .message("Too many active sessions from your location. Please wait for existing sessions to expire.")
                            .build();
                }
            }
            
            // Check for existing session by email
            SessionData existingSession = findActiveSessionByEmail(request.getEmail());
            if (existingSession != null) {
                logger.warn("Active session already exists for email: {}", maskEmail(request.getEmail()));
                
                // Check if the existing session is about to expire (less than 1 minute remaining)
                long sessionAge = System.currentTimeMillis() - existingSession.getCreatedAt();
                long remainingTime = (sessionDurationMinutes * 60 * 1000) - sessionAge;
                
                if (remainingTime < 60000) { // Less than 1 minute
                    logger.info("Existing session about to expire, allowing new session creation");
                    // Remove the expiring session
                    if (existingSession.getSessionToken() != null) {
                        activeSessions.remove(existingSession.getSessionId());
                        sessionTokenToId.remove(existingSession.getSessionToken());
                    } else {
                        pendingSessions.remove(existingSession.getSessionId());
                    }
                } else {
                    // Calculate remaining minutes for user message
                    int remainingMinutes = (int) (remainingTime / 60000);
                    return SessionResponse.builder()
                            .success(false)
                            .message(String.format("An active session already exists. Please wait %d minutes for it to expire or use the existing session.", remainingMinutes))
                            .sessionId(existingSession.getSessionId())
                            .remainingMinutes(remainingMinutes)
                            .build();
                }
            }
            
            // Generate session ID and OTP
            String sessionId = generateSessionId();
            String otp = generateOtp();
            
            logger.info("Generated Session ID: {}", sessionId);
            logger.info("Generated OTP: {}", otp);
            
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
                    .maxAnalysisCount(maxAnalysisPerSession)
                    .clientIp(clientIp)
                    .userAgent(request.getUserAgent())
                    .build();
            
            // Store in pending sessions
            pendingSessions.put(sessionId, sessionData);
            logger.info("Session stored in pending sessions map");
            
            // Track IP to session mapping
            if (clientIp != null && !clientIp.isEmpty()) {
                ipToSessions.computeIfAbsent(clientIp, k -> new ArrayList<>()).add(sessionId);
            }
            
            // Send OTP email
            try {
                logger.info("Calling emailService.sendOtpEmail...");
                emailService.sendOtpEmail(request.getEmail(), request.getName(), otp);
                logger.info("Email service call completed");
            } catch (Exception e) {
                logger.error("Error sending OTP email", e);
                // Don't fail session creation if email fails in development
                if (sendGridApiKey != null && !sendGridApiKey.isEmpty()) {
                    // In production, fail if email cannot be sent
                    pendingSessions.remove(sessionId);
                    if (clientIp != null) {
                        List<String> sessions = ipToSessions.get(clientIp);
                        if (sessions != null) {
                            sessions.remove(sessionId);
                        }
                    }
                    return SessionResponse.builder()
                            .success(false)
                            .message("Failed to send verification email. Please try again.")
                            .build();
                }
            }
            
            // Log session metrics
            int currentTotal = activeSessions.size() + pendingSessions.size();
            logger.info("Current session count: {}/{}", currentTotal, maxConcurrentSessions);
            
            logger.info("=== SESSION CREATED SUCCESSFULLY ===");
            logger.info("Session ID: {}", sessionId);
            logger.info("OTP Code: {}", otp);
            logger.info("=== PLEASE USE THIS OTP TO VERIFY: {} ===", otp);
            
            return SessionResponse.builder()
            	    .success(true)
            	    .message("Session created successfully. Please check your email for the verification code.")
            	    .sessionId(sessionId)
            	    .createdAt(System.currentTimeMillis())
            	    .expiresAt(System.currentTimeMillis() + (sessionDurationMinutes * 60 * 1000)) // milliseconds timestamp
            	    .remainingMinutes(sessionDurationMinutes)
            	    .metadata(SessionResponse.SessionMetadata.builder()
            	        .demoSession(true)
            	        .maxAnalysisCount(maxAnalysisPerSession)
            	        .build())
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
     * Get remaining session time in milliseconds
     */
    public long getRemainingTime(String sessionToken) {
        SessionData sessionData = getSessionByToken(sessionToken);
        if (sessionData == null) {
            return 0;
        }
        
        long sessionAge = System.currentTimeMillis() - sessionData.getCreatedAt();
        long sessionDurationMs = sessionDurationMinutes * 60 * 1000;
        return Math.max(0, sessionDurationMs - sessionAge);
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
    @Scheduled(fixedDelayString = "${demo.session.cleanup.interval:300000}")
    public void cleanupExpiredSessions() {
        logger.debug("Starting session cleanup...");
        
        int cleanedActive = 0;
        int cleanedPending = 0;
        
        // Clean active sessions
        Iterator<Map.Entry<String, SessionData>> activeIterator = activeSessions.entrySet().iterator();
        while (activeIterator.hasNext()) {
            Map.Entry<String, SessionData> entry = activeIterator.next();
            if (isSessionExpired(entry.getValue())) {
                activeIterator.remove();
                sessionTokenToId.remove(entry.getValue().getSessionToken());
                cleanedActive++;
            }
        }
        
        // Clean pending sessions
        Iterator<Map.Entry<String, SessionData>> pendingIterator = pendingSessions.entrySet().iterator();
        while (pendingIterator.hasNext()) {
            Map.Entry<String, SessionData> entry = pendingIterator.next();
            if (isSessionExpired(entry.getValue())) {
                pendingIterator.remove();
                cleanedPending++;
            }
        }
        
        // Clean IP mappings
        ipToSessions.entrySet().removeIf(entry -> {
            entry.getValue().removeIf(sessionId -> 
                !activeSessions.containsKey(sessionId) && !pendingSessions.containsKey(sessionId)
            );
            return entry.getValue().isEmpty();
        });
        
        if (cleanedActive > 0 || cleanedPending > 0) {
            logger.info("Session cleanup completed - Active: {}, Pending: {}, Total remaining: {}", 
                    cleanedActive, cleanedPending, activeSessions.size() + pendingSessions.size());
        }
        
        // Log metrics
        logSessionMetrics();
    }

    private void logSessionMetrics() {
        int totalSessions = activeSessions.size() + pendingSessions.size();
        int uniqueIps = ipToSessions.size();
        
        logger.info("Session metrics - Total: {}/{}, Unique IPs: {}", 
                totalSessions, maxConcurrentSessions, uniqueIps);
        
        // Alert if reaching capacity
        if (totalSessions > maxConcurrentSessions * 0.8) {
            logger.warn("Session capacity warning: {}% full", 
                    (totalSessions * 100) / maxConcurrentSessions);
        }
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
        // Check both active and pending sessions
        SessionData activeSession = activeSessions.values().stream()
                .filter(session -> email.equalsIgnoreCase(session.getEmail()))
                .filter(session -> !isSessionExpired(session))
                .findFirst()
                .orElse(null);
        
        if (activeSession != null) {
            logger.debug("Found active session for email: {}", maskEmail(email));
            return activeSession;
        }
        
        // Also check pending sessions
        SessionData pendingSession = pendingSessions.values().stream()
                .filter(session -> email.equalsIgnoreCase(session.getEmail()))
                .filter(session -> !isSessionExpired(session))
                .findFirst()
                .orElse(null);
        
        if (pendingSession != null) {
            logger.debug("Found pending session for email: {}", maskEmail(email));
            return pendingSession;
        }
        
        return null;
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
     * Session Data inner class WITHOUT Lombok
     */
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
        private int maxAnalysisCount;
        private String clientIp;
        private String userAgent;
        
        // Default constructor
        public SessionData() {
        }
        
        // Builder pattern
        public static SessionDataBuilder builder() {
            return new SessionDataBuilder();
        }
        
        // Getters and setters
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getCompany() { return company; }
        public void setCompany(String company) { this.company = company; }
        
        public String getOtp() { return otp; }
        public void setOtp(String otp) { this.otp = otp; }
        
        public String getSessionToken() { return sessionToken; }
        public void setSessionToken(String sessionToken) { this.sessionToken = sessionToken; }
        
        public long getCreatedAt() { return createdAt; }
        public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
        
        public boolean isVerified() { return verified; }
        public void setVerified(boolean verified) { this.verified = verified; }
        
        public int getAnalysisCount() { return analysisCount; }
        public void setAnalysisCount(int analysisCount) { this.analysisCount = analysisCount; }
        
        public int getOtpAttempts() { return otpAttempts; }
        public void setOtpAttempts(int otpAttempts) { this.otpAttempts = otpAttempts; }
        
        public int getMaxAnalysisCount() { return maxAnalysisCount; }
        public void setMaxAnalysisCount(int maxAnalysisCount) { this.maxAnalysisCount = maxAnalysisCount; }
        
        public String getClientIp() { return clientIp; }
        public void setClientIp(String clientIp) { this.clientIp = clientIp; }
        
        public String getUserAgent() { return userAgent; }
        public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
        
        // Builder class
        public static class SessionDataBuilder {
            private SessionData sessionData = new SessionData();
            
            public SessionDataBuilder sessionId(String sessionId) {
                sessionData.sessionId = sessionId;
                return this;
            }
            
            public SessionDataBuilder email(String email) {
                sessionData.email = email;
                return this;
            }
            
            public SessionDataBuilder name(String name) {
                sessionData.name = name;
                return this;
            }
            
            public SessionDataBuilder company(String company) {
                sessionData.company = company;
                return this;
            }
            
            public SessionDataBuilder otp(String otp) {
                sessionData.otp = otp;
                return this;
            }
            
            public SessionDataBuilder sessionToken(String sessionToken) {
                sessionData.sessionToken = sessionToken;
                return this;
            }
            
            public SessionDataBuilder createdAt(long createdAt) {
                sessionData.createdAt = createdAt;
                return this;
            }
            
            public SessionDataBuilder verified(boolean verified) {
                sessionData.verified = verified;
                return this;
            }
            
            public SessionDataBuilder analysisCount(int analysisCount) {
                sessionData.analysisCount = analysisCount;
                return this;
            }
            
            public SessionDataBuilder otpAttempts(int otpAttempts) {
                sessionData.otpAttempts = otpAttempts;
                return this;
            }
            
            public SessionDataBuilder maxAnalysisCount(int maxAnalysisCount) {
                sessionData.maxAnalysisCount = maxAnalysisCount;
                return this;
            }
            
            public SessionDataBuilder clientIp(String clientIp) {
                sessionData.clientIp = clientIp;
                return this;
            }
            
            public SessionDataBuilder userAgent(String userAgent) {
                sessionData.userAgent = userAgent;
                return this;
            }
            
            public SessionData build() {
                return sessionData;
            }
        }
    }
}