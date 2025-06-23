package com.somdiproy.smartcode.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CodeChunkingService {
    private static final Logger logger = LoggerFactory.getLogger(CodeChunkingService.class);
    
    @Value("${aws.bedrock.max-chunk-size:50000}")
    private int maxChunkSize; // Characters, not bytes
    
    @Value("${aws.bedrock.max-tokens-per-request:100000}")
    private int maxTokensPerRequest;
    
    // Rough estimate: 1 token ≈ 4 characters for code
    private static final double CHARS_PER_TOKEN = 4.0;
    
    public static class CodeChunk {
        private final String content;
        private final int startLine;
        private final int endLine;
        private final String fileName;
        private final int estimatedTokens;
        
        public CodeChunk(String content, int startLine, int endLine, String fileName) {
            this.content = content;
            this.startLine = startLine;
            this.endLine = endLine;
            this.fileName = fileName;
            this.estimatedTokens = (int) (content.length() / CHARS_PER_TOKEN);
        }
        
        // Getters
        public String getContent() { return content; }
        public int getStartLine() { return startLine; }
        public int getEndLine() { return endLine; }
        public String getFileName() { return fileName; }
        public int getEstimatedTokens() { return estimatedTokens; }
    }
    
    public List<CodeChunk> chunkCode(String code, String fileName) {
        List<CodeChunk> chunks = new ArrayList<>();
        
        if (code == null || code.isEmpty()) {
            return chunks;
        }
        
        // Calculate safe chunk size based on token limit
        int safeChunkSize = Math.min(maxChunkSize, (int)(maxTokensPerRequest * CHARS_PER_TOKEN * 0.8)); // 80% safety margin
        
        logger.info("Chunking code of length {} with chunk size {}", code.length(), safeChunkSize);
        
        if (code.length() <= safeChunkSize) {
            // Code fits in one chunk
            int lineCount = countLines(code);
            chunks.add(new CodeChunk(code, 1, lineCount, fileName));
            return chunks;
        }
        
        // Smart chunking: try to break at logical boundaries
        String[] lines = code.split("\n");
        StringBuilder currentChunk = new StringBuilder();
        int currentStartLine = 1;
        int currentLine = 0;
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            
            // Check if adding this line would exceed chunk size
            if (currentChunk.length() + line.length() + 1 > safeChunkSize && currentChunk.length() > 0) {
                // Look for a good break point (method/class boundary)
                int breakPoint = findLogicalBreakPoint(lines, i, Math.min(i + 10, lines.length));
                
                // Create chunk up to break point
                chunks.add(new CodeChunk(
                    currentChunk.toString(),
                    currentStartLine,
                    currentLine,
                    fileName
                ));
                
                // Start new chunk
                currentChunk = new StringBuilder();
                currentStartLine = currentLine + 1;
                
                // Adjust i if we found a better break point
                if (breakPoint > i) {
                    i = breakPoint - 1; // -1 because loop will increment
                    currentLine = breakPoint;
                }
            }
            
            currentChunk.append(line).append("\n");
            currentLine = i + 1;
        }
        
        // Add remaining chunk
        if (currentChunk.length() > 0) {
            chunks.add(new CodeChunk(
                currentChunk.toString(),
                currentStartLine,
                currentLine,
                fileName
            ));
        }
        
        logger.info("Split code into {} chunks", chunks.size());
        return chunks;
    }
    
    private int findLogicalBreakPoint(String[] lines, int start, int end) {
        // Look for method/class boundaries
        Pattern methodPattern = Pattern.compile("^\\s*(public|private|protected|static).*\\{\\s*$");
        Pattern classPattern = Pattern.compile("^\\s*(public|private|protected)?\\s*(class|interface|enum)\\s+\\w+");
        
        for (int i = end - 1; i >= start; i--) {
            String line = lines[i].trim();
            
            // Empty line or closing brace - good break point
            if (line.isEmpty() || line.equals("}")) {
                return i + 1;
            }
            
            // Method or class declaration
            Matcher methodMatcher = methodPattern.matcher(lines[i]);
            Matcher classMatcher = classPattern.matcher(lines[i]);
            
            if (methodMatcher.matches() || classMatcher.matches()) {
                return i;
            }
        }
        
        return start; // No better break point found
    }
    
    private int countLines(String code) {
        return code.split("\n").length;
    }
    
    public boolean isWithinTokenLimit(String content) {
        int estimatedTokens = (int) (content.length() / CHARS_PER_TOKEN);
        return estimatedTokens <= maxTokensPerRequest;
    }
}