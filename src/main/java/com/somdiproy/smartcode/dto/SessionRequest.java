package com.somdiproy.smartcode.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Session Request DTO
 * 
 * Used for creating new demo sessions in the Smart Code Review platform.
 * Contains user information required for session initialization and OTP verification.
 * 
 * @author Somdip Roy
 * @version 1.0.0
 */
public class SessionRequest {
    
	@Pattern(regexp = "^$|^.{2,100}$", message = "Name must be between 2 and 100 characters if provided")
	private String name;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;
    
    @Size(max = 100, message = "Company name must not exceed 100 characters")
    private String company;
    
    @Size(max = 500, message = "Project description must not exceed 500 characters")
    private String projectDescription;
    
    private String preferredLanguage;
    private String timezone;
    private Boolean marketingConsent = false;
    private String referralSource;
    private String clientIp;
    private String userAgent;
    
    // Default constructor
    public SessionRequest() {
    }
    
    // Constructor with all fields
    public SessionRequest(String name, String email, String company, String projectDescription,
                         String preferredLanguage, String timezone, Boolean marketingConsent,
                         String referralSource, String clientIp, String userAgent) {
        this.name = name;
        this.email = email;
        this.company = company;
        this.projectDescription = projectDescription;
        this.preferredLanguage = preferredLanguage;
        this.timezone = timezone;
        this.marketingConsent = marketingConsent;
        this.referralSource = referralSource;
        this.clientIp = clientIp;
        this.userAgent = userAgent;
    }
    
    // Getters and Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getCompany() {
        return company;
    }
    
    public void setCompany(String company) {
        this.company = company;
    }
    
    public String getProjectDescription() {
        return projectDescription;
    }
    
    public void setProjectDescription(String projectDescription) {
        this.projectDescription = projectDescription;
    }
    
    public String getPreferredLanguage() {
        return preferredLanguage;
    }
    
    public void setPreferredLanguage(String preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }
    
    public String getTimezone() {
        return timezone;
    }
    
    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }
    
    public Boolean getMarketingConsent() {
        return marketingConsent;
    }
    
    public void setMarketingConsent(Boolean marketingConsent) {
        this.marketingConsent = marketingConsent;
    }
    
    public String getReferralSource() {
        return referralSource;
    }
    
    public void setReferralSource(String referralSource) {
        this.referralSource = referralSource;
    }
    
    public String getClientIp() {
        return clientIp;
    }
    
    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }
    
    public String getUserAgent() {
        return userAgent;
    }
    
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
}