package com.somdiproy.smartcode.service;

import com.somdiproy.smartcode.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;

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
    
    @Autowired
    private DynamoDBAnalysisStorage dynamoDBStorage;
    
    @Autowired
    private SQSBedrockService sqsService;
    
    /**
     * Analyze uploaded ZIP file
     */
    public AnalysisResponse analyzeZipFile(MultipartFile file, AnalysisRequest request) {
        String analysisId = UUID.randomUUID().toString();
        
        try {
            logger.info("Starting ZIP file analysis: {}", analysisId);
            
            // CRITICAL FIX: Read file content immediately before async processing
            byte[] fileContent = file.getBytes();
            String originalFilename = file.getOriginalFilename();
            String contentType = file.getContentType();
            long fileSize = file.getSize();
            
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
            
            // Process asynchronously with byte array
            CompletableFuture.runAsync(() -> {
                try {
                    processZipFileAnalysisFromBytes(analysisId, fileContent, originalFilename, 
                                                  contentType, fileSize, request);
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
        try {
            // First check in-memory storage
            AnalysisResponse response = analysisStorageService.getAnalysis(analysisId);
            
            // Always check DynamoDB for updates
            logger.debug("Checking DynamoDB for analysis: {}", analysisId);
            
            DynamoDBAnalysisStorage.AnalysisRecord record = 
                dynamoDBStorage.getAnalysis(analysisId);
            
            if (record != null) {
                logger.info("Found in DynamoDB: {} - Status: {}", 
                    analysisId, record.getStatus());
                
                // Create or update response based on DynamoDB record
                AnalysisResponse.AnalysisResponseBuilder builder = 
                    AnalysisResponse.builder()
                        .analysisId(analysisId)
                        .createdAt(record.getTimestamp())
                        .updatedAt(record.getTimestamp());
                
                switch (record.getStatus()) {
                    case "COMPLETED":
                        builder.success(true)
                               .status(AnalysisStatus.COMPLETED)
                               .message("Analysis completed successfully")
                               .result(record.getResult())
                               .progressPercentage(100);
                        break;
                    case "FAILED":
                        builder.success(false)
                               .status(AnalysisStatus.FAILED)
                               .message(record.getMessage() != null ? 
                                   record.getMessage() : "Analysis failed")
                               .progressPercentage(0);
                        break;
                    case "PROCESSING":
                        builder.success(true)
                               .status(AnalysisStatus.PROCESSING)
                               .message("Lambda is processing your code...")
                               .progressPercentage(50);
                        break;
                    case "QUEUED":
                        builder.success(true)
                               .status(AnalysisStatus.PROCESSING)
                               .message("Analysis queued for processing")
                               .progressPercentage(25);
                        break;
                    default:
                        builder.success(true)
                               .status(AnalysisStatus.PROCESSING)
                               .message("Processing...")
                               .progressPercentage(30);
                }
                
                response = builder.build();
                
                // Update in-memory cache if completed or failed
                if (response.getStatus() == AnalysisStatus.COMPLETED || 
                    response.getStatus() == AnalysisStatus.FAILED) {
                    analysisStorageService.storeAnalysis(analysisId, response);
                }
            } else if (response == null) {
                // Not found anywhere
                logger.warn("Analysis not found: {}", analysisId);
                return AnalysisResponse.builder()
                    .success(false)
                    .analysisId(analysisId)
                    .status(AnalysisStatus.FAILED)
                    .message("Analysis not found")
                    .build();
            }
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error retrieving analysis result", e);
            return AnalysisResponse.builder()
                .success(false)
                .analysisId(analysisId)
                .status(AnalysisStatus.FAILED)
                .message("Error retrieving analysis: " + e.getMessage())
                .build();
        }
    }
    
    /**
     * Process ZIP file analysis from byte array (async)
     */
    private void processZipFileAnalysisFromBytes(String analysisId, byte[] fileContent, String filename,
            String contentType, long fileSize, AnalysisRequest request) {
        try {
            updateAnalysisProgress(analysisId, 20, "Uploading file to S3...");

            // Upload to S3 with error handling
            String s3Key = null;
            try {
                // Create an InputStream from byte array for S3 upload
                InputStream inputStream = new ByteArrayInputStream(fileContent);
                s3Key = s3Service.uploadFromInputStream(inputStream, fileSize, filename, contentType, analysisId);
                logger.info("File uploaded to S3 with key: {}", s3Key);
            } catch (Exception e) {
                logger.error("S3 upload failed, continuing with analysis", e);
                // Continue analysis even if S3 upload fails
            }

            updateAnalysisProgress(analysisId, 40, "Extracting code from ZIP...");

            // Extract code from byte array and count lines
            String extractedCode = extractCodeFromZipBytes(fileContent, analysisId);
            
            // Count lines of code
            int linesOfCode = extractedCode.split("\n").length;
            logger.info("Extracted {} lines of code from ZIP", linesOfCode);

            updateAnalysisProgress(analysisId, 60, "Running static analysis...");

            updateAnalysisProgress(analysisId, 80, "Running AI analysis...");

            // Create metadata to pass to SQS
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("fileName", filename);
            metadata.put("fileSize", fileSize);
            metadata.put("uploadTimestamp", LocalDateTime.now().toString());
            metadata.put("linesOfCode", linesOfCode);
            
            // Submit to Bedrock processing queue with metadata
            if (sqsService != null) {
                sqsService.submitAnalysisRequest(analysisId, extractedCode, request.getLanguage(), metadata);
            } else {
                // Fallback to direct Bedrock submission
                bedrockService.submitAnalysisWithId(analysisId, extractedCode, request.getLanguage());
            }
            
            // Store metadata in analysis response
            AnalysisResponse currentResponse = analysisStorageService.getAnalysis(analysisId);
            if (currentResponse != null) {
                // Create initial result with metadata
                CodeReviewResult initialResult = new CodeReviewResult();
                initialResult.setMetadata(metadata);
                currentResponse.setResult(initialResult);
                analysisStorageService.storeAnalysis(analysisId, currentResponse);
                
                // Also save to DynamoDB
                dynamoDBStorage.saveAnalysisStatus(analysisId, "QUEUED", "Submitted to processing queue");
            }
            
            updateAnalysisProgress(analysisId, 85, "Submitted to AI processing queue");

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
            
            // Count lines for pasted code
            int linesOfCode = code.split("\n").length;
            
            // Create metadata for pasted code
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("fileName", "pasted-code." + (request.getLanguage() != null ? request.getLanguage() : "txt"));
            metadata.put("analysisType", "code-paste");
            metadata.put("linesOfCode", linesOfCode);
            metadata.put("uploadTimestamp", LocalDateTime.now().toString());
            
            // Submit to processing queue with metadata
            if (sqsService != null) {
                sqsService.submitAnalysisRequest(analysisId, code, request.getLanguage(), metadata);
            } else {
                // Fallback to direct Bedrock submission
                bedrockService.submitAnalysisWithId(analysisId, code, request.getLanguage());
            }

            updateAnalysisProgress(analysisId, 85, "Submitted to AI processing queue");

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
            dynamoDBStorage.saveAnalysisStatus(analysisId, "FAILED", errorMessage);
            current.setUpdatedAt(System.currentTimeMillis());
            analysisStorageService.storeAnalysis(analysisId, current);
        }
    }
    
    /**
     * Extract code from ZIP bytes
     */
    private String extractCodeFromZipBytes(byte[] zipContent, String analysisId) {
        StringBuilder extractedCode = new StringBuilder();
        
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipContent))) {
            ZipEntry zipEntry;
            
            while ((zipEntry = zis.getNextEntry()) != null) {
                if (!zipEntry.isDirectory() && isCodeFile(zipEntry.getName())) {
                    // Read file content
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int len;
                    
                    while ((len = zis.read(buffer)) > 0) {
                        baos.write(buffer, 0, len);
                    }
                    
                    String fileContent = baos.toString("UTF-8");
                    extractedCode.append("// File: ").append(zipEntry.getName()).append("\n");
                    extractedCode.append(fileContent).append("\n\n");
                }
                zis.closeEntry();
            }
        } catch (Exception e) {
            logger.error("Error extracting code from ZIP", e);
            throw new RuntimeException("Failed to extract code from ZIP", e);
        }
        
        return extractedCode.toString();
    }

    /**
     * Check if file is a code file based on extension
     */
    private boolean isCodeFile(String fileName) {
        String[] codeExtensions = {
            ".java", ".py", ".js", ".ts", ".cpp", ".c", ".cs", ".go", 
            ".rb", ".php", ".swift", ".kt", ".rs", ".scala", ".html", 
            ".css", ".xml", ".json", ".yaml", ".yml", ".sql", ".sh",
            ".bat", ".ps1", ".r", ".m", ".dart", ".vue", ".jsx", ".tsx"
        };
        
        String lowerFileName = fileName.toLowerCase();
        for (String ext : codeExtensions) {
            if (lowerFileName.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }
}