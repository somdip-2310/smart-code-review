package com.somdiproy.smartcode.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * RestTemplate Configuration for Smart Code Review
 * 
 * Configures RestTemplate bean for HTTP client operations
 * 
 * @author Somdip Roy
 */
@Configuration
public class RestTemplateConfig {
    
    /**
     * Create a RestTemplate bean with custom configuration
     * 
     * @param builder RestTemplateBuilder provided by Spring Boot
     * @return Configured RestTemplate instance
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                // Set connection and read timeouts
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(30))
                // Add error handler
                .errorHandler(new CustomRestTemplateErrorHandler())
                // Add interceptors for logging if needed
                .interceptors((request, body, execution) -> {
                    // Log request details
                    request.getHeaders().add("User-Agent", "SmartCodeReview/1.0");
                    return execution.execute(request, body);
                })
                .build();
    }
    
    /**
     * Alternative RestTemplate with custom request factory
     * Uncomment if you need more control over HTTP connections
     */
    /*
    @Bean
    public RestTemplate customRestTemplate() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(10000); // 10 seconds
        factory.setReadTimeout(30000); // 30 seconds
        factory.setConnectionRequestTimeout(5000); // 5 seconds
        
        RestTemplate restTemplate = new RestTemplate(factory);
        
        // Add message converters if needed
        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
        
        return restTemplate;
    }
    */
}