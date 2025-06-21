package com.somdiproy.smartcode.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Code Analysis Request DTO
 * 
 * @author Somdip Roy
 */
public class CodeAnalysisRequest {
    
    @NotBlank(message = "Session token is required")
    private String sessionToken;
    
    @NotBlank(message = "Code is required")
    @Size(max = 100000, message = "Code must be less than 100KB")
    private String code;
    
    @NotBlank(message = "Language is required")
    private String language;
    
    private String fileName;
    
    // Default constructor
    public CodeAnalysisRequest() {
    }
    
    // Constructor with all fields
    public CodeAnalysisRequest(String sessionToken, String code, String language, String fileName) {
        this.sessionToken = sessionToken;
        this.code = code;
        this.language = language;
        this.fileName = fileName;
    }
    
    // Getters and Setters
    public String getSessionToken() {
        return sessionToken;
    }
    
    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getLanguage() {
        return language;
    }
    
    public void setLanguage(String language) {
        this.language = language;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}