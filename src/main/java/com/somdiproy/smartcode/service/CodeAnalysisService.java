package com.somdiproy.smartcode.service;

import com.somdiproy.smartcode.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Code Analysis Service
 * 
 * Handles code analysis operations including:
 * - ZIP file analysis
 * - Direct code paste analysis
 * - Asynchronous processing
 * - Progress tracking
 * 
 * @author Somdip Roy
 */
@Service
public class CodeAnalysisService {
    
    private static final Logger logger = LoggerFactory.getLogger(CodeAnalysisService.class);
    
    @Autowired
    private BedrockService bedrockService;
    
    @Autowired
    private S3Service s3Service;
    
    @Autowired
    private AnalysisStorageService analysisStorageService;
    
    /**
     * Analyze uploaded ZIP file
     */
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
    
    /**
     * Analyze pasted code
     */
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
    
    /**
     * Get analysis result by ID
     */
    public AnalysisResponse getAnalysisResult(String analysisId) {
        AnalysisResponse response = analysisStorageService.getAnalysis(analysisId);
        
        if (response == null) {
            return AnalysisResponse.builder()
                    .success(false)
                    .analysisId(analysisId)
                    .status(AnalysisStatus.FAILED)
                    .message("Analysis not found")
                    .build();
        }
        
        return response;
    }
    
    /**
     * Process ZIP file analysis (async)
     */
    private void processZipFileAnalysis(String analysisId, MultipartFile file, AnalysisRequest request) {
        try {
            updateAnalysisProgress(analysisId, 20, "Uploading file to S3...");
            
            // Upload to S3
            String s3Key = s3Service.uploadFile(file, analysisId);
            
            updateAnalysisProgress(analysisId, 40, "Extracting code from ZIP...");
            
            // Extract code from ZIP
            String extractedCode = extractCodeFromZip(file);
            
            updateAnalysisProgress(analysisId, 60, "Running static analysis...");
            
            updateAnalysisProgress(analysisId, 80, "Running AI analysis...");
            
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
                    .s3Key(s3Key)
                    .build();
            
            analysisStorageService.storeAnalysis(analysisId, finalResponse);
            
        } catch (Exception e) {
            logger.error("Error processing ZIP file analysis", e);
            markAnalysisAsFailed(analysisId, e.getMessage());
        }
    }
    
    /**
     * Process code analysis (async)
     */
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
    
    /**
     * Update analysis progress
     */
    private void updateAnalysisProgress(String analysisId, int percentage, String message) {
        AnalysisResponse current = analysisStorageService.getAnalysis(analysisId);
        if (current != null) {
            current.setProgressPercentage(percentage);
            current.setMessage(message);
            current.setUpdatedAt(System.currentTimeMillis());
            analysisStorageService.storeAnalysis(analysisId, current);
        }
    }
    
    /**
     * Mark analysis as failed
     */
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
    
    /**
     * Extract code from ZIP file
     */
    private String extractCodeFromZip(MultipartFile file) {
        // TODO: Implement ZIP extraction logic using zip4j
        // This is a placeholder - implement actual extraction
        return "// Extracted code placeholder\n// Implement ZIP extraction logic here";
    }
}