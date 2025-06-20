package com.somdiproy.smartcode.service;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class FileProcessingService {
    
    private static final Logger logger = LoggerFactory.getLogger(FileProcessingService.class);
    
    private static final List<String> SUPPORTED_EXTENSIONS = Arrays.asList(
        ".java", ".js", ".ts", ".jsx", ".tsx", ".py", ".cpp", ".c", ".h", ".hpp",
        ".cs", ".php", ".rb", ".go", ".rs", ".kt", ".swift", ".scala", ".clj",
        ".sql", ".html", ".css", ".scss", ".sass", ".xml", ".json", ".yaml", ".yml",
        ".md", ".txt", ".sh", ".bat", ".ps1", ".dockerfile", ".properties"
    );
    
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
    private static final int MAX_FILES = 100;
    private static final int MAX_CONTENT_LENGTH = 500000; // 500KB combined content
    
    public ProcessedCode processZipFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("ZIP file is empty");
        }
        
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("ZIP file too large. Maximum size: 50MB");
        }
        
        List<String> fileNames = new ArrayList<>();
        StringBuilder combinedContent = new StringBuilder();
        int fileCount = 0;
        
        try (ZipArchiveInputStream zipStream = new ZipArchiveInputStream(
                new ByteArrayInputStream(file.getBytes()))) {
            
            ZipArchiveEntry entry;
            while ((entry = zipStream.getNextZipEntry()) != null) {
                
                if (entry.isDirectory()) {
                    continue;
                }
                
                String fileName = entry.getName();
                
                // Skip hidden files and directories
                if (fileName.contains("/.") || fileName.startsWith(".")) {
                    continue;
                }
                
                // Check if file extension is supported
                if (!isSupportedFile(fileName)) {
                    continue;
                }
                
                // Limit number of files
                if (++fileCount > MAX_FILES) {
                    logger.warn("Too many files in ZIP. Processing first {} files", MAX_FILES);
                    break;
                }
                
                try {
                    byte[] content = IOUtils.toByteArray(zipStream);
                    String fileContent = new String(content, StandardCharsets.UTF_8);
                    
                    // Skip binary files or very large files
                    if (fileContent.length() > 50000) { // 50KB per file
                        logger.warn("Skipping large file: {} ({}KB)", fileName, fileContent.length() / 1024);
                        continue;
                    }
                    
                    fileNames.add(fileName);
                    combinedContent.append("// File: ").append(fileName).append("\n");
                    combinedContent.append(fileContent);
                    combinedContent.append("\n\n");
                    
                    // Limit total content size
                    if (combinedContent.length() > MAX_CONTENT_LENGTH) {
                        logger.warn("Combined content too large. Truncating at {}KB", MAX_CONTENT_LENGTH / 1024);
                        break;
                    }
                    
                } catch (Exception e) {
                    logger.warn("Error processing file {}: {}", fileName, e.getMessage());
                }
            }
        }
        
        if (fileNames.isEmpty()) {
            throw new IllegalArgumentException("No supported code files found in ZIP archive");
        }
        
        logger.info("Processed {} files from ZIP archive", fileNames.size());
        
        return new ProcessedCode(fileNames, combinedContent.toString());
    }
    
    private boolean isSupportedFile(String fileName) {
        String lowerCaseFileName = fileName.toLowerCase();
        return SUPPORTED_EXTENSIONS.stream().anyMatch(lowerCaseFileName::endsWith);
    }
    
    public static class ProcessedCode {
        private final List<String> fileNames;
        private final String combinedContent;
        
        public ProcessedCode(List<String> fileNames, String combinedContent) {
            this.fileNames = fileNames;
            this.combinedContent = combinedContent;
        }
        
        public List<String> getFileNames() { return fileNames; }
        public String getCombinedContent() { return combinedContent; }
    }
}