package com.somdiproy.smartcode.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Session Request DTO
 * 
 * Used for creating new demo sessions in the Smart Code Review platform.
 * Contains user information required for session initialization and OTP verification.
 * 
 * @author Somdip Roy
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionRequest {
    
    /**
     * User's full name
     * Required for personalized email communication
     */
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;
    
    /**
     * User's email address
     * Used for OTP delivery and session identification
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;
    
    /**
     * User's company/organization name
     * Optional field for tracking usage patterns
     */
    @Size(max = 100, message = "Company name must not exceed 100 characters")
    private String company;
    
    /**
     * Brief description of the project/code to be analyzed
     * Optional field to understand user context
     */
    @Size(max = 500, message = "Project description must not exceed 500 characters")
    private String projectDescription;
    
    /**
     * Programming language of the code to be analyzed
     * Optional field for pre-configuration
     */
    private String preferredLanguage;
    
    /**
     * User's timezone for session scheduling
     * Optional field, defaults to UTC
     */
    private String timezone;
    
    /**
     * Consent for receiving updates about the service
     * Optional field, defaults to false
     */
    @Builder.Default
    private Boolean marketingConsent = false;
    
    /**
     * Source of how user found the service
     * Optional field for analytics
     */
    private String referralSource;
    
    /**
     * Client IP address (populated by the controller)
     * Used for security and rate limiting
     */
    private String clientIp;
    
    /**
     * User agent string (populated by the controller)
     * Used for compatibility checking
     */
    private String userAgent;
}