package com.somdiproy.smartcode.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Smart Code Review View Controller
 * Handles Thymeleaf template rendering for the frontend
 * 
 * @author Somdip Roy
 */
@Controller
@RequestMapping("/")
public class SmartCodeViewController {
    
    @Value("${google.analytics.measurement-id:G-TJMD3KM77H}")
    private String googleAnalyticsId;
    
    @Value("${spring.application.name:Smart Code Review}")
    private String applicationName;
    
    /**
     * Add common attributes to all views in this controller
     */
    @ModelAttribute
    public void addCommonAttributes(Model model, HttpServletRequest request) {
        model.addAttribute("requestURI", request.getRequestURI());
        model.addAttribute("requestURL", request.getRequestURL().toString());
        model.addAttribute("contextPath", request.getContextPath());
    }
    
    /**
     * Home page - Main landing page
     */
    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("title", "Smart Code Review - AI-Powered Code Analysis Platform | Somdip Roy");
        model.addAttribute("description", "Professional AI-powered code review service using Amazon Bedrock. Get instant security analysis, performance optimization, and code quality insights. Free 7-minute demo available.");
        model.addAttribute("currentPage", "home");
        model.addAttribute("googleAnalyticsId", googleAnalyticsId);
        model.addAttribute("applicationName", applicationName);
        
        // Add structured data for SEO
        model.addAttribute("structuredData", generateHomeStructuredData());
        
        return "smartcode/index";
    }
    
    /**
     * Upload page - File upload interface
     */
    @GetMapping("/upload")
    public String upload(Model model) {
        model.addAttribute("title", "Upload Code for Analysis - Smart Code Review | Somdip Roy");
        model.addAttribute("description", "Upload your ZIP files, GitHub repositories, or code archives for comprehensive AI-powered analysis. Supports Java, Python, JavaScript, and more programming languages.");
        model.addAttribute("currentPage", "upload");
        model.addAttribute("googleAnalyticsId", googleAnalyticsId);
        
        return "smartcode/upload";
    }
    
    /**
     * Analyze page - Github Connect
     */
    @GetMapping("/github-connect")
    public String githubConnect(Model model) {
        model.addAttribute("title", "Connect GitHub - Smart Code Review | Somdip Roy");
        model.addAttribute("description", "Set up automated code analysis with GitHub webhooks for continuous integration.");
        model.addAttribute("currentPage", "github");
        model.addAttribute("googleAnalyticsId", googleAnalyticsId);
        
        // Generate session-specific webhook URL if user has active session
        // Add logic to retrieve session token
        
        return "smartcode/github-connect";
    }
    
    /**
     * Generate structured data for home page SEO
     */
    private String generateHomeStructuredData() {
        return """
            {
                "@context": "https://schema.org",
                "@type": "SoftwareApplication",
                "name": "Smart Code Review",
                "applicationCategory": "DeveloperApplication",
                "operatingSystem": "Web Browser",
                "url": "https://smartcode.somdip.dev",
                "description": "AI-powered code review and analysis platform using Amazon Bedrock",
                "offers": {
                    "@type": "Offer",
                    "price": "0",
                    "priceCurrency": "USD",
                    "description": "Free 7-minute demo available"
                },
                "creator": {
                    "@type": "Person",
                    "name": "Somdip Roy",
                    "jobTitle": "Senior Technical Architect"
                }
            }
            """;
    }
}