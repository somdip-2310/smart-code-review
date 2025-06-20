package com.somdiproy.smartcode.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
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
     * Analyze page - Code paste interface
     */
    @GetMapping("/analyze")
    public String analyze(Model model) {
        model.addAttribute("title", "Paste Code for Analysis - Smart Code Review | Somdip Roy");
        model.addAttribute("description", "Paste your code directly for instant AI-powered analysis. Get security insights, performance recommendations, and quality metrics in minutes.");
        model.addAttribute("currentPage", "analyze");
        model.addAttribute("googleAnalyticsId", googleAnalyticsId);
        
        return "smartcode/analysis";
    }
    
    /**
     * Results page - Analysis results display
     */
    @GetMapping("/results")
    public String results(Model model) {
        model.addAttribute("title", "Code Analysis Results - Smart Code Review | Somdip Roy");
        model.addAttribute("description", "View comprehensive code analysis results including security vulnerabilities, performance bottlenecks, and improvement recommendations.");
        model.addAttribute("currentPage", "results");
        model.addAttribute("googleAnalyticsId", googleAnalyticsId);
        
        return "smartcode/results";
    }
    
    /**
     * Demo page - Interactive demo
     */
    @GetMapping("/demo")
    public String demo(Model model) {
        model.addAttribute("title", "Try Smart Code Review Demo - Free AI Code Analysis | Somdip Roy");
        model.addAttribute("description", "Experience our AI-powered code review platform with a free 7-minute demo. No registration required. Analyze your code instantly with Amazon Bedrock.");
        model.addAttribute("currentPage", "demo");
        model.addAttribute("googleAnalyticsId", googleAnalyticsId);
        
        return "smartcode/demo";
    }
    
    /**
     * Features page - Detailed feature overview
     */
    @GetMapping("/features")
    public String features(Model model) {
        model.addAttribute("title", "Features - Smart Code Review Platform | Somdip Roy");
        model.addAttribute("description", "Explore comprehensive features of our AI-powered code review platform: security scanning, performance analysis, quality metrics, and intelligent recommendations.");
        model.addAttribute("currentPage", "features");
        model.addAttribute("googleAnalyticsId", googleAnalyticsId);
        
        return "smartcode/features";
    }
    
    /**
     * API Documentation page
     */
    @GetMapping("/docs")
    public String docs(Model model) {
        model.addAttribute("title", "API Documentation - Smart Code Review | Somdip Roy");
        model.addAttribute("description", "Complete API documentation for Smart Code Review service integration. Learn how to integrate AI-powered code analysis into your development workflow.");
        model.addAttribute("currentPage", "docs");
        model.addAttribute("googleAnalyticsId", googleAnalyticsId);
        
        return "smartcode/docs";
    }
    
    /**
     * About page
     */
    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("title", "About Smart Code Review - AI Code Analysis by Somdip Roy");
        model.addAttribute("description", "Learn about Smart Code Review, an AI-powered code analysis platform built by Somdip Roy, Senior Technical Architect with 13+ years of experience in Java and AWS.");
        model.addAttribute("currentPage", "about");
        model.addAttribute("googleAnalyticsId", googleAnalyticsId);
        
        // Add author information
        model.addAttribute("authorName", "Somdip Roy");
        model.addAttribute("authorTitle", "Senior Technical Architect");
        model.addAttribute("authorExperience", "13+ years");
        model.addAttribute("authorPortfolio", "https://somdip.dev");
        
        return "smartcode/about";
    }
    
    /**
     * Privacy Policy page
     */
    @GetMapping("/privacy")
    public String privacy(Model model) {
        model.addAttribute("title", "Privacy Policy - Smart Code Review | Somdip Roy");
        model.addAttribute("description", "Smart Code Review privacy policy. Learn how we protect your code and data during AI-powered analysis sessions.");
        model.addAttribute("currentPage", "privacy");
        model.addAttribute("googleAnalyticsId", googleAnalyticsId);
        
        return "smartcode/privacy";
    }
    
    /**
     * Terms of Service page
     */
    @GetMapping("/terms")
    public String terms(Model model) {
        model.addAttribute("title", "Terms of Service - Smart Code Review | Somdip Roy");
        model.addAttribute("description", "Terms of service for Smart Code Review platform. Review usage guidelines for our AI-powered code analysis service.");
        model.addAttribute("currentPage", "terms");
        model.addAttribute("googleAnalyticsId", googleAnalyticsId);
        
        return "smartcode/terms";
    }
    
    /**
     * Sitemap XML
     */
    @GetMapping(value = "/sitemap.xml", produces = "application/xml")
    public String sitemap(Model model) {
        model.addAttribute("baseUrl", "https://smartcode.somdip.dev");
        model.addAttribute("lastModified", java.time.LocalDate.now().toString());
        
        return "smartcode/sitemap";
    }
    
    /**
     * Robots.txt
     */
    @GetMapping(value = "/robots.txt", produces = "text/plain")
    public String robots(Model model) {
        model.addAttribute("sitemapUrl", "https://smartcode.somdip.dev/sitemap.xml");
        
        return "smartcode/robots";
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
                "description": "AI-powered code review and analysis platform using Amazon Bedrock for security, performance, and quality insights.",
                "offers": {
                    "@type": "Offer",
                    "price": "0",
                    "priceCurrency": "USD",
                    "description": "Free 7-minute demo available"
                },
                "creator": {
                    "@type": "Person",
                    "name": "Somdip Roy",
                    "jobTitle": "Senior Technical Architect",
                    "url": "https://somdip.dev",
                    "sameAs": [
                        "https://www.linkedin.com/in/somdip-roy-b8004b111/"
                    ]
                },
                "provider": {
                    "@type": "Organization",
                    "name": "Somdip Roy Portfolio",
                    "url": "https://somdip.dev"
                },
                "softwareVersion": "1.0.0",
                "applicationSubCategory": "Code Analysis Tool",
                "featureList": [
                    "AI-powered code analysis",
                    "Security vulnerability detection",
                    "Performance optimization suggestions",
                    "Code quality metrics",
                    "Multi-language support",
                    "Real-time analysis results"
                ],
                "screenshot": "https://smartcode.somdip.dev/images/smart-code-screenshot.jpg",
                "video": {
                    "@type": "VideoObject",
                    "name": "Smart Code Review Demo",
                    "description": "See how Smart Code Review analyzes your code with AI",
                    "thumbnailUrl": "https://smartcode.somdip.dev/images/demo-thumbnail.jpg"
                }
            }
            """;
    }
}