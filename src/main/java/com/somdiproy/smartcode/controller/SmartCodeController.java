package com.somdiproy.smartcode.controller;

import com.somdiproy.smartcode.model.CodeAnalysisRequest;
import com.somdiproy.smartcode.model.CodeAnalysisResult;
import com.somdiproy.smartcode.service.BedrockCodeAnalysisService;
import com.somdiproy.smartcode.service.FileProcessingService;
import com.somdiproy.smartcode.service.SessionManagementService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"https://smartcode.somdip.dev", "http://localhost:3000"})
public class SmartCodeController {
    
    private static final Logger logger = LoggerFactory.getLogger(SmartCodeController.class);
    
    @Autowired
    private SessionManagementService sessionManagementService;
    
    @Autowired
    private BedrockCodeAnalysisService bedrockCodeAnalysisService;
    
    @Autowired
    private FileProcessingService fileProcessingService;
    
    // Session management endpoints
    @PostMapping("/session/request-verification")
    public ResponseEntity<Map<String, Object>> requestEmailVerification(@RequestParam String email) {
        SessionManagementService.EmailVerificationResult result = 
            sessionManagementService.sendVerificationCode(email);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", result.isSuccess());
        response.put("message", result.getMessage());
        
        return result.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }
    
    @PostMapping("/session/verify-email")
    public ResponseEntity<Map<String, Object>> verifyEmail(
            @RequestParam String email, 
            @RequestParam String code) {
        
        SessionManagementService.EmailVerificationResult result = 
            sessionManagementService.verifyEmail(email, code);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", result.isSuccess());
        response.put("message", result.getMessage());
        
        if (result.isSuccess()) {
            response.put("sessionId", result.getSessionId());
            response.put("token", result.getToken());
            response.put("sessionDuration", 7); // minutes
        }
        
        return result.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }
    
    @PostMapping("/session/end")
    public ResponseEntity<Map<String, Object>> endSession(@RequestParam String sessionId) {
        sessionManagementService.endSession(sessionId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Session ended successfully");
        
        return ResponseEntity.ok(response);
    }
    
    // Code analysis endpoints
    @PostMapping("/analyze/zip")
    public ResponseEntity<CodeAnalysisResult> analyzeZipFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("sessionId") String sessionId) {
        
        try {
            // Validate session
            if (!sessionManagementService.isSessionValid(sessionId)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            // Process the ZIP file
            FileProcessingService.ProcessedCode processedCode = 
                fileProcessingService.processZipFile(file);
            
            // Analyze the code
            CodeAnalysisResult result = bedrockCodeAnalysisService.analyzeCode(
                sessionId, 
                processedCode.getCombinedContent(), 
                processedCode.getFileNames()
            );
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error analyzing ZIP file for session: {}", sessionId, e);
            
            CodeAnalysisResult errorResult = new CodeAnalysisResult();
            errorResult.setSessionId(sessionId);
            errorResult.setStatus(CodeAnalysisResult.AnalysisStatus.FAILED);
            errorResult.setSummary("Failed to process ZIP file: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }
    
    @PostMapping("/analyze/code")
    public ResponseEntity<CodeAnalysisResult> analyzeCodeContent(
            @Valid @RequestBody CodeAnalysisRequest request) {
        
        try {
            // Validate session
            if (!sessionManagementService.isSessionValid(request.getSessionId())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            // Analyze the code
            CodeAnalysisResult result = bedrockCodeAnalysisService.analyzeCode(
                request.getSessionId(),
                request.getCodeContent(),
                request.getFileNames()
            );
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error analyzing code content for session: {}", request.getSessionId(), e);
            
            CodeAnalysisResult errorResult = new CodeAnalysisResult();
            errorResult.setSessionId(request.getSessionId());
            errorResult.setStatus(CodeAnalysisResult.AnalysisStatus.FAILED);
            errorResult.setSummary("Failed to analyze code: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }
    
    @PostMapping("/webhook/github")
    public ResponseEntity<Map<String, Object>> handleGitHubWebhook(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "X-GitHub-Event", required = false) String event) {
        
        try {
            logger.info("Received GitHub webhook event: {}", event);
            
            // Process GitHub webhook (implementation depends on your requirements)
            // This is a placeholder - you'll need to implement based on your needs
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Webhook received successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error processing GitHub webhook", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Webhook processing failed");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "smart-code-review");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }
}