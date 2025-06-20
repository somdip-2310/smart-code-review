package com.somdiproy.smartcode.service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class EmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    @Value("${sendgrid.api.key:}")
    private String sendGridApiKey;
    
    @Value("${sendgrid.from.email:smartcode@somdip.dev}")
    private String fromEmail;
    
    @Value("${sendgrid.from.name:Smart Code Review}")
    private String fromName;
    
    public boolean sendOtpEmail(String toEmail, String otpCode) {
        if (sendGridApiKey == null || sendGridApiKey.trim().isEmpty()) {
            logger.error("SendGrid API key not configured");
            return false;
        }
        
        try {
            Email from = new Email(fromEmail, fromName);
            Email to = new Email(toEmail);
            String subject = "Your Smart Code Review Verification Code";
            
            String htmlContent = buildOtpEmailTemplate(otpCode);
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
                return true;
            } else {
                logger.error("Failed to send OTP email. Status: {}, Body: {}", 
                           response.getStatusCode(), response.getBody());
                return false;
            }
            
        } catch (IOException e) {
            logger.error("Error sending OTP email to: {}", maskEmail(toEmail), e);
            return false;
        }
    }
    
    private String buildOtpEmailTemplate(String otpCode) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Smart Code Review - Verification Code</title>
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 0; background-color: #f5f5f5; }
                    .container { max-width: 600px; margin: 0 auto; background-color: white; }
                    .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 40px 20px; text-align: center; }
                    .header h1 { margin: 0; font-size: 28px; }
                    .content { padding: 40px 20px; }
                    .otp-code { background: #f8f9fa; border: 2px solid #667eea; padding: 20px; margin: 20px 0; text-align: center; border-radius: 8px; }
                    .otp-code .code { font-size: 32px; font-weight: bold; color: #667eea; letter-spacing: 8px; font-family: monospace; }
                    .instructions { background: #e3f2fd; padding: 20px; border-radius: 8px; margin: 20px 0; }
                    .footer { background: #f8f9fa; padding: 20px; text-align: center; color: #666; font-size: 14px; }
                    .button { display: inline-block; background: #667eea; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: bold; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🚀 Smart Code Review</h1>
                        <p>AI-Powered Code Analysis Platform</p>
                    </div>
                    <div class="content">
                        <h2>Your Verification Code</h2>
                        <p>Welcome to Smart Code Review! Use the following 6-digit code to start your demo session:</p>
                        
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
                        
                        <p><strong>⏰ This code expires in 5 minutes</strong></p>
                        
                        <p style="margin-top: 30px;">
                            <a href="https://smartcode.somdip.dev" class="button">Go to Smart Code Review</a>
                        </p>
                        
                        <hr style="margin: 30px 0; border: none; border-top: 1px solid #eee;">
                        
                        <h3>🔍 What You'll Experience:</h3>
                        <ul>
                            <li><strong>Security Analysis:</strong> Identify vulnerabilities and security issues</li>
                            <li><strong>Code Quality:</strong> Get suggestions for better code practices</li>
                            <li><strong>Performance Review:</strong> Find optimization opportunities</li>
                            <li><strong>AI-Powered Insights:</strong> Powered by Amazon Bedrock</li>
                        </ul>
                    </div>
                    <div class="footer">
                        <p>This is an automated message from Smart Code Review by Somdip Roy</p>
                        <p>If you didn't request this code, please ignore this email</p>
                        <p>© 2025 Somdip Roy Portfolio | <a href="https://somdip.dev">somdip.dev</a></p>
                    </div>
                </div>
            </body>
            </html>
            """, otpCode);
    }
    
    private String maskEmail(String email) {
        if (email == null || email.length() < 3) return "***";
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) return "***";
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }
}