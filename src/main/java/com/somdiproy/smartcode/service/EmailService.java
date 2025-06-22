package com.somdiproy.smartcode.service;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

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
    
    @Value("${sendgrid.api.key:}")
    private String sendGridApiKey;
    
    @Value("${sendgrid.from.email:smartcode@somdip.dev}")
    private String fromEmail;
    
    @Value("${sendgrid.from.name:Smart Code Review}")
    private String fromName;
    
    /**
     * Send OTP email for session verification
     */
    public void sendOtpEmail(String toEmail, String userName, String otp) {
        try {
            // Check if SendGrid is configured (production)
            if (sendGridApiKey != null && !sendGridApiKey.trim().isEmpty()) {
                // Production: Send actual email via SendGrid
                logger.info("Sending OTP email via SendGrid to: {}", maskEmail(toEmail));
                
                Email from = new Email(fromEmail, fromName);
                Email to = new Email(toEmail);
                String subject = "Your Smart Code Review Verification Code";
                String htmlContent = buildOtpEmailTemplate(otp, userName);
                Content content = new Content("text/html", htmlContent);
                Mail mail = new Mail(from, subject, to, content);
                
                SendGrid sg = new SendGrid(sendGridApiKey);
                Request request = new Request();
                request.setMethod(Method.POST);
                request.setEndpoint("mail/send");
                request.setBody(mail.build());
                
                Response response = sg.api(request);
                
                if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                    logger.info("OTP email sent successfully to: {}", maskEmail(toEmail));
                } else {
                    logger.error("Failed to send OTP email. Status: {}, Body: {}", 
                        response.getStatusCode(), response.getBody());
                    throw new RuntimeException("Failed to send email");
                }
            } else {
                // Development: Log OTP to console
                logger.info("=== DEVELOPMENT MODE - OTP EMAIL ===");
                logger.info("Environment: Development (SendGrid not configured)");
                logger.info("To: {}", toEmail);
                logger.info("User: {}", userName);
                logger.info("Subject: Your Smart Code Review Verification Code");
                logger.info("OTP Code: {}", otp);
                logger.info("===================================");
                logger.info("NOTE: In production, this will be sent via SendGrid email");
            }
            
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
            
            // Check if SendGrid is configured
            if (sendGridApiKey != null && !sendGridApiKey.trim().isEmpty()) {
                // Production: Send actual welcome email
                Email from = new Email(fromEmail, fromName);
                Email to = new Email(toEmail);
                String subject = "Welcome to Smart Code Review!";
                String htmlContent = buildWelcomeEmailTemplate();
                Content content = new Content("text/html", htmlContent);
                Mail mail = new Mail(from, subject, to, content);
                
                SendGrid sg = new SendGrid(sendGridApiKey);
                Request request = new Request();
                request.setMethod(Method.POST);
                request.setEndpoint("mail/send");
                request.setBody(mail.build());
                
                Response response = sg.api(request);
                
                if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                    logger.info("Welcome email sent successfully to: {}", maskEmail(toEmail));
                } else {
                    logger.error("Failed to send welcome email. Status: {}", response.getStatusCode());
                }
            } else {
                // Development: Log welcome email
                logger.info("=== DEMO WELCOME EMAIL ===");
                logger.info("To: {}", toEmail);
                logger.info("Subject: Welcome to Smart Code Review!");
                logger.info("Content: Your session is now active. You have 7 minutes to explore our AI-powered code analysis.");
                logger.info("==========================");
            }
            
        } catch (Exception e) {
            logger.error("Failed to send welcome email to: {}", maskEmail(toEmail), e);
            // Don't throw exception for welcome email failures
        }
    }
    
    /**
     * Build OTP email template
     */
    private String buildOtpEmailTemplate(String otpCode, String userName) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Smart Code Review - Verification Code</title>
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 0; background-color: #f4f4f4; }
                    .container { max-width: 600px; margin: 0 auto; background-color: #ffffff; }
                    .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 40px 20px; text-align: center; }
                    .header h1 { margin: 0; font-size: 28px; }
                    .content { padding: 40px 20px; }
                    .otp-code { background: #f8f9fa; border: 2px solid #667eea; padding: 20px; margin: 20px 0; text-align: center; border-radius: 8px; }
                    .otp-code .code { font-size: 32px; font-weight: bold; color: #667eea; letter-spacing: 5px; }
                    .instructions { background: #e3f2fd; padding: 20px; border-radius: 8px; margin: 20px 0; }
                    .footer { background: #f8f9fa; padding: 20px; text-align: center; color: #666; }
                    .button { display: inline-block; background: #667eea; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🚀 Smart Code Review</h1>
                        <p>AI-Powered Code Analysis Platform</p>
                    </div>
                    <div class="content">
                        <h2>Hello%s,</h2>
                        <p>Thank you for signing up for Smart Code Review! Use the following verification code to activate your demo session:</p>
                        <div class="otp-code">
                            <div class="code">%s</div>
                        </div>
                        <div class="instructions">
                            <h3>📋 What's Next?</h3>
                            <ul>
                                <li>Enter this code on the Smart Code Review website</li>
                                <li>Your demo session will last for 7 minutes</li>
                                <li>Upload code files or paste code directly for AI analysis</li>
                                <li>Get comprehensive security and quality insights</li>
                            </ul>
                        </div>
                        <p><strong>⏰ This code expires in 10 minutes</strong></p>
                        <p style="margin-top: 30px;">
                            <a href="https://smartcode.somdip.dev" class="button">Go to Smart Code Review</a>
                        </p>
                    </div>
                    <div class="footer">
                        <p>© 2025 Smart Code Review by Somdip Roy. All rights reserved.</p>
                        <p>This is an automated message. Please do not reply to this email.</p>
                    </div>
                </div>
            </body>
            </html>
            """, userName != null ? " " + userName : "", otpCode);
    }
    
    /**
     * Build welcome email template
     */
    private String buildWelcomeEmailTemplate() {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Welcome to Smart Code Review</title>
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 0; background-color: #f4f4f4; }
                    .container { max-width: 600px; margin: 0 auto; background-color: #ffffff; }
                    .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 40px 20px; text-align: center; }
                    .header h1 { margin: 0; font-size: 28px; }
                    .content { padding: 40px 20px; }
                    .feature { background: #f8f9fa; padding: 15px; margin: 10px 0; border-radius: 8px; }
                    .footer { background: #f8f9fa; padding: 20px; text-align: center; color: #666; }
                    .button { display: inline-block; background: #667eea; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🎉 Welcome to Smart Code Review!</h1>
                        <p>Your session is now active</p>
                    </div>
                    <div class="content">
                        <h2>Your 7-Minute Demo Has Started!</h2>
                        <p>You now have full access to our AI-powered code analysis platform. Here's what you can do:</p>
                        
                        <div class="feature">
                            <h3>📤 Upload Code</h3>
                            <p>Upload ZIP files containing your source code for comprehensive analysis</p>
                        </div>
                        
                        <div class="feature">
                            <h3>📝 Paste Code</h3>
                            <p>Paste code snippets directly for instant AI-powered review</p>
                        </div>
                        
                        <div class="feature">
                            <h3>🔗 GitHub Integration</h3>
                            <p>Connect your GitHub repository for automated code analysis</p>
                        </div>
                        
                        <p style="margin-top: 30px;">
                            <a href="https://smartcode.somdip.dev/upload" class="button">Start Code Analysis</a>
                        </p>
                        
                        <p><strong>⏰ Remember:</strong> Your demo session expires in 7 minutes. Make the most of it!</p>
                    </div>
                    <div class="footer">
                        <p>© 2025 Smart Code Review by Somdip Roy. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """;
    }
    
    /**
     * Mask email for privacy in logs
     */
    private String maskEmail(String email) {
        if (email == null || email.length() < 3) return "***";
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) return "***";
        return email.substring(0, 1) + "***" + email.substring(atIndex);
    }
}