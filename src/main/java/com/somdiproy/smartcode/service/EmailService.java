package com.somdiproy.smartcode.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Email Service for Smart Code Review
 * 
 * Handles sending OTP emails and notifications for the demo sessions.
 * This is a mock implementation for development. In production, integrate
 * with SendGrid, AWS SES, or your preferred email service.
 * 
 * @author Somdip Roy
 */
@Service
public class EmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    @Value("${sendgrid.from.email:smartcode@somdip.dev}")
    private String fromEmail;
    
    @Value("${sendgrid.from.name:Smart Code Review}")
    private String fromName;
    
    /**
     * Send OTP email for session verification
     */
    public void sendOtpEmail(String toEmail, String userName, String otp) {
        try {
            // Mock implementation - in production, integrate with actual email service
            logger.info("Sending OTP email to: {} with code: {}", maskEmail(toEmail), otp);
            
            // For demo purposes, log the OTP (remove in production)
            logger.info("=== DEMO OTP EMAIL ===");
            logger.info("To: {}", toEmail);
            logger.info("Subject: Your Smart Code Review Verification Code");
            logger.info("OTP Code: {}", otp);
            logger.info("======================");
            
            // TODO: Implement actual email sending with SendGrid/SES
            // sendGridClient.send(createOtpEmail(toEmail, userName, otp));
            
        } catch (Exception e) {
            logger.error("Failed to send OTP email to: {}", maskEmail(toEmail), e);
            throw new RuntimeException("Failed to send verification email", e);
        }
    }
    
    /**
     * Send welcome email after session verification
     */
    public void sendWelcomeEmail(String toEmail) {
        try {
            logger.info("Sending welcome email to: {}", maskEmail(toEmail));
            
            // Mock implementation
            logger.info("=== DEMO WELCOME EMAIL ===");
            logger.info("To: {}", toEmail);
            logger.info("Subject: Welcome to Smart Code Review!");
            logger.info("Content: Your session is now active. You have 7 minutes to explore our AI-powered code analysis.");
            logger.info("==========================");
            
        } catch (Exception e) {
            logger.error("Failed to send welcome email to: {}", maskEmail(toEmail), e);
            // Don't throw exception for welcome email failures
        }
    }
    
    private String maskEmail(String email) {
        if (email == null || email.length() < 3) return "***";
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) return "***";
        return email.substring(0, 1) + "***" + email.substring(atIndex);
    }
}