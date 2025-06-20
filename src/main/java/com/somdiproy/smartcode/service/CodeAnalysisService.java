// ===== CodeAnalysisService.java =====
package com.somdiproy.smartcode.service;

import com.somdiproy.smartcode.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class CodeAnalysisService {
    
    private static final Logger logger = LoggerFactory.getLogger(CodeAnalysisService.class);
    
    @Autowired
    private BedrockService bedrockService;
    
    @Autowired
    private S3Service s3Service;
    
    @Autowired
    private AnalysisStorageService analysisStorageService;
    
    public AnalysisResponse analyzeZipFile(MultipartFile file, AnalysisRequest request) {
        String analysisId = UUID.randomUUID().toString();
        
        try {
            logger.info("Starting ZIP file analysis: {}", analysisId);
            
            // Create initial response
            AnalysisResponse response = AnalysisResponse.builder()
                    .success(true)
                    .analysisId(analysisId)
                    .status(AnalysisStatus.PROCESSING)
                    .message("Analysis started")
                    .createdAt(System.currentTimeMillis())
                    .progressPercentage(10)
                    .build();
            
            // Store initial status
            analysisStorageService.storeAnalysis(analysisId, response);
            
            // Process asynchronously
            CompletableFuture.runAsync(() -> {
                try {
                    processZipFileAnalysis(analysisId, file, request);
                } catch (Exception e) {
                    logger.error("Error in async ZIP analysis", e);
                    markAnalysisAsFailed(analysisId, e.getMessage());
                }
            });
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error starting ZIP analysis", e);
            return AnalysisResponse.builder()
                    .success(false)
                    .analysisId(analysisId)
                    .status(AnalysisStatus.FAILED)
                    .message("Failed to start analysis: " + e.getMessage())
                    .build();
        }
    }
    
    public AnalysisResponse analyzeCode(String code, AnalysisRequest request) {
        String analysisId = UUID.randomUUID().toString();
        
        try {
            logger.info("Starting code analysis: {}", analysisId);
            
            // Create initial response
            AnalysisResponse response = AnalysisResponse.builder()
                    .success(true)
                    .analysisId(analysisId)
                    .status(AnalysisStatus.PROCESSING)
                    .message("Analysis started")
                    .createdAt(System.currentTimeMillis())
                    .progressPercentage(10)
                    .build();
            
            // Store initial status
            analysisStorageService.storeAnalysis(analysisId, response);
            
            // Process asynchronously
            CompletableFuture.runAsync(() -> {
                try {
                    processCodeAnalysis(analysisId, code, request);
                } catch (Exception e) {
                    logger.error("Error in async code analysis", e);
                    markAnalysisAsFailed(analysisId, e.getMessage());
                }
            });
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error starting code analysis", e);
            return AnalysisResponse.builder()
                    .success(false)
                    .analysisId(analysisId)
                    .status(AnalysisStatus.FAILED)
                    .message("Failed to start analysis: " + e.getMessage())
                    .build();
        }
    }
    
    public AnalysisResponse getAnalysisResult(String analysisId) {
        return analysisStorageService.getAnalysis(analysisId);
    }
    
    private void processZipFileAnalysis(String analysisId, MultipartFile file, AnalysisRequest request) {
        try {
            // Update progress
            updateAnalysisProgress(analysisId, 25, "Extracting files...");
            
            // Extract and upload to S3
            String s3Key = s3Service.uploadZipFile(file, analysisId);
            
            updateAnalysisProgress(analysisId, 50, "Analyzing code structure...");
            
            // Extract code from ZIP
            String extractedCode = extractCodeFromZip(file);
            
            updateAnalysisProgress(analysisId, 75, "Running AI analysis...");
            
            // Analyze with Bedrock
            CodeReviewResult result = bedrockService.analyzeCode(extractedCode, request.getLanguage());
            
            updateAnalysisProgress(analysisId, 100, "Analysis completed");
            
            // Mark as completed
            AnalysisResponse finalResponse = AnalysisResponse.builder()
                    .success(true)
                    .analysisId(analysisId)
                    .status(AnalysisStatus.COMPLETED)
                    .message("Analysis completed successfully")
                    .result(result)
                    .createdAt(System.currentTimeMillis())
                    .updatedAt(System.currentTimeMillis())
                    .progressPercentage(100)
                    .build();
            
            analysisStorageService.storeAnalysis(analysisId, finalResponse);
            
        } catch (Exception e) {
            logger.error("Error processing ZIP file analysis", e);
            markAnalysisAsFailed(analysisId, e.getMessage());
        }
    }
    
    private void processCodeAnalysis(String analysisId, String code, AnalysisRequest request) {
        try {
            updateAnalysisProgress(analysisId, 25, "Preparing code analysis...");
            
            updateAnalysisProgress(analysisId, 50, "Running static analysis...");
            
            updateAnalysisProgress(analysisId, 75, "Running AI analysis...");
            
            // Analyze with Bedrock
            CodeReviewResult result = bedrockService.analyzeCode(code, request.getLanguage());
            
            updateAnalysisProgress(analysisId, 100, "Analysis completed");
            
            // Mark as completed
            AnalysisResponse finalResponse = AnalysisResponse.builder()
                    .success(true)
                    .analysisId(analysisId)
                    .status(AnalysisStatus.COMPLETED)
                    .message("Analysis completed successfully")
                    .result(result)
                    .createdAt(System.currentTimeMillis())
                    .updatedAt(System.currentTimeMillis())
                    .progressPercentage(100)
                    .build();
            
            analysisStorageService.storeAnalysis(analysisId, finalResponse);
            
        } catch (Exception e) {
            logger.error("Error processing code analysis", e);
            markAnalysisAsFailed(analysisId, e.getMessage());
        }
    }
    
    private void updateAnalysisProgress(String analysisId, int percentage, String message) {
        AnalysisResponse current = analysisStorageService.getAnalysis(analysisId);
        if (current != null) {
            current.setProgressPercentage(percentage);
            current.setMessage(message);
            current.setUpdatedAt(System.currentTimeMillis());
            analysisStorageService.storeAnalysis(analysisId, current);
        }
    }
    
    private void markAnalysisAsFailed(String analysisId, String errorMessage) {
        AnalysisResponse current = analysisStorageService.getAnalysis(analysisId);
        if (current != null) {
            current.setSuccess(false);
            current.setStatus(AnalysisStatus.FAILED);
            current.setMessage("Analysis failed: " + errorMessage);
            current.setUpdatedAt(System.currentTimeMillis());
            analysisStorageService.storeAnalysis(analysisId, current);
        }
    }
    
    private String extractCodeFromZip(MultipartFile file) {
        // TODO: Implement ZIP extraction logic
        return "// Extracted code placeholder";
    }
}

// ===== SessionService.java =====
package com.somdiproy.smartcode.service;

import com.somdiproy.smartcode.dto.OtpVerificationRequest;
import com.somdiproy.smartcode.dto.SessionRequest;
import com.somdiproy.smartcode.dto.SessionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionService {
    
    private static final Logger logger = LoggerFactory.getLogger(SessionService.class);
    private static final int SESSION_DURATION_MINUTES = 7;
    private static final Map<String, SessionData> sessions = new ConcurrentHashMap<>();
    private static final Map<String, String> sessionTokens = new ConcurrentHashMap<>();
    
    @Autowired
    private EmailService emailService;
    
    public SessionResponse createSession(SessionRequest request) {
        try {
            String sessionId = UUID.randomUUID().toString();
            String otp = generateOtp();
            
            SessionData sessionData = SessionData.builder()
                    .sessionId(sessionId)
                    .email(request.getEmail())
                    .name(request.getName())
                    .otp(otp)
                    .createdAt(System.currentTimeMillis())
                    .verified(false)
                    .build();
            
            sessions.put(sessionId, sessionData);
            
            // Send OTP email
            emailService.sendOtpEmail(request.getEmail(), otp, request.getName());
            
            return SessionResponse.builder()
                    .success(true)
                    .sessionId(sessionId)
                    .message("OTP sent to your email")
                    .remainingMinutes(SESSION_DURATION_MINUTES)
                    .build();
            
        } catch (Exception e) {
            logger.error("Error creating session", e);
            return SessionResponse.builder()
                    .success(false)
                    .message("Failed to create session: " + e.getMessage())
                    .build();
        }
    }
    
    public SessionResponse verifyOtp(OtpVerificationRequest request) {
        try {
            SessionData sessionData = sessions.get(request.getSessionId());
            
            if (sessionData == null) {
                return SessionResponse.builder()
                        .success(false)
                        .message("Session not found or expired")
                        .build();
            }
            
            if (isSessionExpired(sessionData)) {
                sessions.remove(request.getSessionId());
                return SessionResponse.builder()
                        .success(false)
                        .message("Session expired")
                        .build();
            }
            
            if (!sessionData.getOtp().equals(request.getOtp())) {
                return SessionResponse.builder()
                        .success(false)
                        .message("Invalid OTP")
                        .build();
            }
            
            // Generate session token
            String sessionToken = UUID.randomUUID().toString();
            sessionData.setVerified(true);
            sessionData.setSessionToken(sessionToken);
            sessionTokens.put(sessionToken, request.getSessionId());
            
            long expiresAt = sessionData.getCreatedAt() + (SESSION_DURATION_MINUTES * 60 * 1000);
            
            return SessionResponse.builder()
                    .success(true)
                    .sessionId(request.getSessionId())
                    .sessionToken(sessionToken)
                    .message("Session verified successfully")
                    .expiresAt(expiresAt)
                    .remainingMinutes(calculateRemainingMinutes(sessionData))
                    .build();
            
        } catch (Exception e) {
            logger.error("Error verifying OTP", e);
            return SessionResponse.builder()
                    .success(false)
                    .message("Failed to verify OTP: " + e.getMessage())
                    .build();
        }
    }
    
    public boolean isValidSession(String sessionToken) {
        try {
            String sessionId = sessionTokens.get(sessionToken);
            if (sessionId == null) {
                return false;
            }
            
            SessionData sessionData = sessions.get(sessionId);
            if (sessionData == null || !sessionData.isVerified()) {
                return false;
            }
            
            if (isSessionExpired(sessionData)) {
                cleanupSession(sessionId, sessionToken);
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            logger.error("Error validating session", e);
            return false;
        }
    }
    
    public int getRemainingTime(String sessionToken) {
        String sessionId = sessionTokens.get(sessionToken);
        if (sessionId == null) {
            return 0;
        }
        
        SessionData sessionData = sessions.get(sessionId);
        if (sessionData == null) {
            return 0;
        }
        
        return calculateRemainingMinutes(sessionData);
    }
    
    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }
    
    private boolean isSessionExpired(SessionData sessionData) {
        long currentTime = System.currentTimeMillis();
        long sessionAge = currentTime - sessionData.getCreatedAt();
        return sessionAge > (SESSION_DURATION_MINUTES * 60 * 1000);
    }
    
    private int calculateRemainingMinutes(SessionData sessionData) {
        long currentTime = System.currentTimeMillis();
        long sessionAge = currentTime - sessionData.getCreatedAt();
        long remainingTime = (SESSION_DURATION_MINUTES * 60 * 1000) - sessionAge;
        return Math.max(0, (int) (remainingTime / (60 * 1000)));
    }
    
    private void cleanupSession(String sessionId, String sessionToken) {
        sessions.remove(sessionId);
        sessionTokens.remove(sessionToken);
    }
    
    // Inner class for session data
    private static class SessionData {
        private String sessionId;
        private String email;
        private String name;
        private String otp;
        private String sessionToken;
        private long createdAt;
        private boolean verified;
        
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