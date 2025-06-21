package com.somdiproy.smartcode;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

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

    public static void main(String[] args) {
        System.out.println("🚀 Starting Smart Code Review Service...");
        System.out.println("📊 Service: smartcode.somdip.dev");
        System.out.println("🤖 AI Engine: Amazon Bedrock");
        System.out.println("🔧 Tech Stack: Spring Boot 3 + Java 17");
        
        SpringApplication.run(SmartCodeReviewApplication.class, args);
        
        System.out.println("✅ Smart Code Review Service started successfully!");
    }
}