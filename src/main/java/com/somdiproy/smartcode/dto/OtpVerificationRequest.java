package com.somdiproy.smartcode.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * OTP Verification Request DTO
 * 
 * @author Somdip Roy
 */
public class OtpVerificationRequest {
    
    @NotBlank(message = "Session ID is required")
    private String sessionId;
    
    @NotBlank(message = "OTP is required")
    private String otp;
    
    // Default constructor
    public OtpVerificationRequest() {
    }
    
    // Constructor with all fields
    public OtpVerificationRequest(String sessionId, String otp) {
        this.sessionId = sessionId;
        this.otp = otp;
    }
    
    // Getters and Setters
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getOtp() {
        return otp;
    }
    
    public void setOtp(String otp) {
        this.otp = otp;
    }
}