package com.somdiproy.smartcode.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    
    private static final Logger logger = LoggerFactory.getLogger(SmartCodeViewController.class);
    
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
    public String githubConnect(Model model, HttpServletRequest request) {
        model.addAttribute("title", "Connect GitHub - Smart Code Review | Somdip Roy");
        model.addAttribute("description", "Set up automated code analysis with GitHub webhooks for continuous integration.");
        model.addAttribute("currentPage", "github");
        model.addAttribute("googleAnalyticsId", googleAnalyticsId);
        
        // Check for session token in request parameter or session
        String sessionToken = request.getParameter("sessionToken");
        if (sessionToken == null || sessionToken.isEmpty()) {
            // Try to get from session attribute
            sessionToken = (String) request.getSession().getAttribute("sessionToken");
        }
        
        // Generate webhook URL with actual session token
        if (sessionToken != null && !sessionToken.isEmpty()) {
            String webhookUrl = String.format("https://smartcode.somdip.dev/api/v1/github/webhook/%s", sessionToken);
            model.addAttribute("webhookUrl", webhookUrl);
            model.addAttribute("sessionToken", sessionToken);
        } else {
            model.addAttribute("webhookUrl", "https://smartcode.somdip.dev/api/v1/github/webhook/{session-token}");
            model.addAttribute("sessionToken", null);
        }
        
        return "smartcode/github-connect";
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
        
        return "smartcode/analyze";
    }
    
    /**
     * Results page - Display analysis results
     */
    @GetMapping("/results/{analysisId}")
    public String results(@PathVariable String analysisId, Model model) {
        model.addAttribute("title", "Analysis Results - Smart Code Review | Somdip Roy");
        model.addAttribute("description", "View detailed code analysis results with security insights, performance metrics, and quality recommendations.");
        model.addAttribute("currentPage", "results");
        model.addAttribute("analysisId", analysisId);
        model.addAttribute("googleAnalyticsId", googleAnalyticsId);
        model.addAttribute("applicationName", applicationName);
        
        logger.info("Rendering results page for analysis: {}", analysisId);
        
        return "smartcode/results";
    }
    
    @GetMapping("/compare/{analysisId1}/{analysisId2}")
    public String compareAnalyses(@PathVariable String analysisId1, 
                                @PathVariable String analysisId2, 
                                Model model) {
        model.addAttribute("title", "Compare Analyses - Smart Code Review");
        model.addAttribute("analysisId1", analysisId1);
        model.addAttribute("analysisId2", analysisId2);
        model.addAttribute("googleAnalyticsId", googleAnalyticsId);
        return "smartcode/compare";
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