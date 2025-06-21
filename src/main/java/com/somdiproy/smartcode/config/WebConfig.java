// ===== WebConfig.java =====
package com.somdiproy.smartcode.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve static resources
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
        
        // Serve uploaded files (if needed)
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}