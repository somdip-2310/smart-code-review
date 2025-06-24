package com.somdiproy.smartcode;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.util.TimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Smart Code Review Service - Main Application
 * 
 * AI-Powered Code Analysis and Review Platform
 * Part of Somdip Roy's Microservice Architecture
 * 
 * @author Somdip Roy
 * @version 1.0.0
 * @since 2025
 */
@SpringBootApplication
@EnableScheduling
@EnableCaching
@EnableAsync
public class SmartCodeReviewApplication {
    
    private static final Logger logger = LoggerFactory.getLogger(SmartCodeReviewApplication.class);

    public static void main(String[] args) {
        // CRITICAL: Set timezone to UTC before any AWS operations
        System.setProperty("user.timezone", "UTC");
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        
        // Log timezone information
        logger.info("System timezone set to: {}", TimeZone.getDefault().getID());
        logger.info("Current time: {}", new java.util.Date());
        
        System.out.println("🚀 Starting Smart Code Review Service...");
        System.out.println("📊 Service: smartcode.somdip.dev");
        System.out.println("🤖 AI Engine: Amazon Bedrock");
        System.out.println("🔧 Tech Stack: Spring Boot 3 + Java 17");
        System.out.println("🕐 Timezone: " + TimeZone.getDefault().getID());
        
        SpringApplication.run(SmartCodeReviewApplication.class, args);
        
        System.out.println("✅ Smart Code Review Service started successfully!");
    }
}