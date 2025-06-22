package com.somdiproy.smartcode.controller;

import com.somdiproy.smartcode.dto.*;
import com.somdiproy.smartcode.service.CodeAnalysisService;
import com.somdiproy.smartcode.service.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * Code Review Controller - Main REST API
 * 
 * Handles all code analysis requests including:
 * - ZIP file uploads
 * - Direct code paste
 * - GitHub webhook integration
 * - Session management
 * 
 * @author Somdip Roy
 */
@RestController
@RequestMapping("/api/v1/code-review")
@CrossOrigin(origins = {"https://smartcode.somdip.dev", "http://localhost:3000"})
public class CodeReviewController {
    
    private static final Logger logger = LoggerFactory.getLogger(CodeReviewController.class);
    
    @Autowired
    private CodeAnalysisService codeAnalysisService;
    
    @Autowired
    private SessionService sessionService;
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "smart-code-review");
        health.put("version", "1.0.0");
        health.put("timestamp", System.currentTimeMillis());
        health.put("capabilities", new String[]{
            "zip-upload", "code-paste", "github-webhook", 
            "ai-analysis", "security-scan", "performance-check"
        });
        
        logger.info("Health check requested");
        return ResponseEntity.ok(health);
    }
    
    /**
     * Create a new demo session
     */
    @PostMapping("/session/create")
    public ResponseEntity<SessionResponse> createSession(@Valid @RequestBody SessionRequest request) {
        try {
            logger.info("Creating new session for email: {}", request.getEmail());
            
            SessionResponse session = sessionService.createSession(request);
            
            logger.info("Session created successfully: {}", session.getSessionId());
            return ResponseEntity.ok(session);
            
        } catch (Exception e) {
            logger.error("Error creating session", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(SessionResponse.builder()
                            .success(false)
                            .message("Failed to create session: " + e.getMessage())
                            .build());
        }
    }
    
    /**
     * Verify OTP and activate session
     */
    @PostMapping("/session/verify")
    public ResponseEntity<SessionResponse> verifySession(@Valid @RequestBody OtpVerificationRequest request) {
        try {
            logger.info("Verifying OTP for session: {}", request.getSessionId());
            
            SessionResponse response = sessionService.verifyOtp(request);
            
            if (response.isSuccess()) {
                logger.info("Session verified successfully: {}", request.getSessionId());
            } else {
                logger.warn("Session verification failed: {}", request.getSessionId());
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error verifying session", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(SessionResponse.builder()
                            .success(false)
                            .message("Failed to verify session: " + e.getMessage())
                            .build());
        }
    }
    
    /**
     * Upload ZIP file for analysis
     */
    @PostMapping("/analyze/zip")
    public ResponseEntity<AnalysisResponse> analyzeZipFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("sessionToken") String sessionToken,
            HttpServletRequest request) {
        
        try {
            logger.info("ZIP file upload received: {} ({})", file.getOriginalFilename(), file.getSize());
            
            // Validate session
            if (!sessionService.isValidSession(sessionToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(AnalysisResponse.builder()
                                .success(false)
                                .message("Invalid or expired session")
                                .build());
            }
            
            // Validate file
            if (file.isEmpty() || file.getSize() > 50 * 1024 * 1024) { // 50MB limit
                return ResponseEntity.badRequest()
                        .body(AnalysisResponse.builder()
                                .success(false)
                                .message("File is empty or too large (max 50MB)")
                                .build());
            }
            
            // Process analysis
            AnalysisRequest analysisRequest = AnalysisRequest.builder()
                    .type(AnalysisType.ZIP_UPLOAD)
                    .sessionToken(sessionToken)
                    .fileName(file.getOriginalFilename())
                    .fileSize(file.getSize())
                    .clientIp(getClientIp(request))
                    .build();
            
            AnalysisResponse response = codeAnalysisService.analyzeZipFile(file, analysisRequest);
            
            logger.info("ZIP analysis completed: {}", response.getAnalysisId());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error analyzing ZIP file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AnalysisResponse.builder()
                            .success(false)
                            .message("Failed to analyze file: " + e.getMessage())
                            .build());
        }
    }
    
    /**
     * Analyze pasted code
     */
    @PostMapping("/analyze/code")
    public ResponseEntity<AnalysisResponse> analyzeCode(@Valid @RequestBody CodeAnalysisRequest request,
                                                       HttpServletRequest httpRequest) {
        try {
            logger.info("Code paste analysis received for language: {}", request.getLanguage());
            
            // Validate session
            if (!sessionService.isValidSession(request.getSessionToken())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(AnalysisResponse.builder()
                                .success(false)
                                .message("Invalid or expired session")
                                .build());
            }
            
            // Validate code length
            if (request.getCode().length() > 100000) { // 100KB limit
                return ResponseEntity.badRequest()
                        .body(AnalysisResponse.builder()
                                .success(false)
                                .message("Code is too large (max 100KB)")
                                .build());
            }
            
            AnalysisRequest analysisRequest = AnalysisRequest.builder()
                    .type(AnalysisType.CODE_PASTE)
                    .sessionToken(request.getSessionToken())
                    .language(request.getLanguage())
                    .clientIp(getClientIp(httpRequest))
                    .build();
            
            AnalysisResponse response = codeAnalysisService.analyzeCode(request.getCode(), analysisRequest);
            
            logger.info("Code analysis completed: {}", response.getAnalysisId());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error analyzing code", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AnalysisResponse.builder()
                            .success(false)
                            .message("Failed to analyze code: " + e.getMessage())
                            .build());
        }
    }
    
    /**
     * Get analysis result by ID
     */
    @GetMapping("/analysis/{analysisId}")
    public ResponseEntity<AnalysisResponse> getAnalysis(@PathVariable String analysisId,
                                                       @RequestParam String sessionToken) {
        try {
            logger.info("Retrieving analysis: {}", analysisId);
            
            // Validate session
            if (!sessionService.isValidSession(sessionToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(AnalysisResponse.builder()
                                .success(false)
                                .message("Invalid or expired session")
                                .build());
            }
            
            AnalysisResponse response = codeAnalysisService.getAnalysisResult(analysisId);
            
            if (response == null) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error retrieving analysis", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AnalysisResponse.builder()
                            .success(false)
                            .message("Failed to retrieve analysis: " + e.getMessage())
                            .build());
        }
    }
    
    /**
     * GitHub webhook endpoint
     */
    @PostMapping("/webhook/github")
    public ResponseEntity<Map<String, Object>> githubWebhook(@RequestBody Map<String, Object> payload,
                                                            HttpServletRequest request) {
        try {
            logger.info("GitHub webhook received");
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "received");
            response.put("message", "Webhook processed successfully");
            response.put("timestamp", System.currentTimeMillis());
            
            // TODO: Implement GitHub webhook processing
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error processing GitHub webhook", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @PostMapping("/session/test")
    public ResponseEntity<Map<String, Object>> testSession() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Test endpoint working");
        response.put("timestamp", System.currentTimeMillis());
        
        // Generate test OTP
        String testOtp = String.format("%06d", (int)(Math.random() * 1000000));
        response.put("otp", testOtp);
        
        logger.info("=== TEST SESSION ENDPOINT ===");
        logger.info("Generated test OTP: {}", testOtp);
        logger.info("===========================");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get session status
     */
    @GetMapping("/session/status")
    public ResponseEntity<Map<String, Object>> getSessionStatus(@RequestParam String sessionToken) {
        try {
            boolean isValid = sessionService.isValidSession(sessionToken);
            Map<String, Object> status = new HashMap<>();
            status.put("valid", isValid);
            status.put("timestamp", System.currentTimeMillis());
            
            if (isValid) {
                // Add session details if valid
                status.put("remainingTime", sessionService.getRemainingTime(sessionToken));
            }
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            logger.error("Error checking session status", e);
            Map<String, Object> error = new HashMap<>();
            error.put("valid", false);
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Extract client IP from request
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}