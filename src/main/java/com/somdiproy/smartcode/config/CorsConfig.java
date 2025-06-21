package com.somdiproy.smartcode.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Additional CORS Configuration for Smart Code Review
 * 
 * Provides WebMvcConfigurer for CORS mappings.
 * The main CORS configuration is handled in SecurityConfig.
 * 
 * @author Somdip Roy
 */
@Configuration
public class CorsConfig {
    
    /**
     * Configure additional CORS mappings for the application
     * This works alongside the CORS configuration in SecurityConfig
     */
    @Bean
    public WebMvcConfigurer additionalCorsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                // Additional CORS mappings for specific endpoints
                
                // GitHub webhook endpoints - allow all origins since webhooks come from GitHub
                registry.addMapping("/api/v1/github/webhook/**")
                    .allowedOrigins("*")
                    .allowedMethods("POST", "GET")
                    .allowedHeaders("*")
                    .exposedHeaders("X-Request-Id", "X-Rate-Limit-Remaining")
                    .allowCredentials(false)
                    .maxAge(3600);
                
                // Health check endpoints - allow all origins without credentials
                registry.addMapping("/actuator/**")
                    .allowedOrigins("*")
                    .allowedMethods("GET")
                    .allowCredentials(false)
                    .maxAge(3600);
                
                // Public API endpoints with specific configuration
                registry.addMapping("/api/v1/code-review/health")
                    .allowedOrigins("*")
                    .allowedMethods("GET")
                    .allowCredentials(false);
                
                // Session endpoints with more restrictive CORS
                registry.addMapping("/api/v1/code-review/session/**")
                    .allowedOrigins(
                        "http://localhost:8083",
                        "https://smartcode.somdip.dev",
                        "https://www.smartcode.somdip.dev"
                    )
                    .allowedMethods("GET", "POST", "OPTIONS")
                    .allowedHeaders("Content-Type", "X-Requested-With")
                    .exposedHeaders("X-Session-Token", "X-Session-Expires")
                    .allowCredentials(true)
                    .maxAge(3600);
            }
        };
    }
}