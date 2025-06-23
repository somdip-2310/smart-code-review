package com.somdiproy.smartcode.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.util.concurrent.RateLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;


/**
 * Rate Limiting Configuration for Smart Code Review
 * 
 * Implements multiple layers of rate limiting:
 * - Session creation rate limiting
 * - API endpoint rate limiting
 * - Failed attempt tracking
 * - IP-based throttling
 * 
 * @author Somdip Roy
 */
@EnableScheduling
@Configuration
public class RateLimitConfig implements WebMvcConfigurer {
    
    @Value("${rate.limit.session.per.minute:10}")
    private double sessionCreationRatePerMinute;
    
    @Value("${rate.limit.api.per.minute:60}")
    private double apiCallsPerMinute;
    
    @Value("${rate.limit.analysis.per.minute:5}")
    private double analysisRatePerMinute;
    
    @Value("${rate.limit.failed.attempts.max:5}")
    private int maxFailedAttempts;
    
    @Value("${rate.limit.failed.attempts.window.minutes:15}")
    private int failedAttemptWindowMinutes;
    
    /**
     * Rate limiter for session creation
     * Prevents abuse of session creation endpoint
     */
    @Bean
    public RateLimiter sessionCreationRateLimiter() {
        // Allow configured session creations per minute
        return RateLimiter.create(sessionCreationRatePerMinute / 60.0);
    }
    
    /**
     * Rate limiter for general API calls
     * Applies to all authenticated endpoints
     */
    @Bean
    public RateLimiter apiRateLimiter() {
        // Allow configured API calls per minute
        return RateLimiter.create(apiCallsPerMinute / 60.0);
    }
    
    /**
     * Rate limiter for code analysis requests
     * More restrictive as analysis is resource-intensive
     */
    @Bean
    public RateLimiter analysisRateLimiter() {
        // Allow configured analysis requests per minute
        return RateLimiter.create(analysisRatePerMinute / 60.0);
    }
    
    /**
     * Cache for tracking failed login/OTP attempts
     * Key: email or IP address
     * Value: number of failed attempts
     */
    @Bean
    public Cache<String, Integer> failedAttemptCache() {
        return Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(failedAttemptWindowMinutes, TimeUnit.MINUTES)
                .build();
    }
    
    /**
     * Cache for tracking API usage per session
     * Key: sessionToken
     * Value: APIUsageStats
     */
    @Bean
    public Cache<String, APIUsageStats> apiUsageCache() {
        return Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .build();
    }
    
    /**
     * IP-based rate limiter map
     * Separate rate limiter per IP for fair usage
     */
    @Bean
    public ConcurrentHashMap<String, RateLimiter> ipRateLimiters() {
        return new ConcurrentHashMap<>();
    }
    
    /**
     * Get or create rate limiter for specific IP
     */
    public RateLimiter getIpRateLimiter(String ipAddress) {
        ConcurrentHashMap<String, RateLimiter> limiters = ipRateLimiters();
        return limiters.computeIfAbsent(ipAddress, 
            ip -> RateLimiter.create(sessionCreationRatePerMinute / 60.0));
    }
    
    /**
     * Register rate limiting interceptor
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor())
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/v1/code-review/health");
    }
    
    /**
     * Rate limiting interceptor bean
     */
    @Bean
    public RateLimitInterceptor rateLimitInterceptor() {
        return new RateLimitInterceptor(
            sessionCreationRateLimiter(),
            apiRateLimiter(),
            analysisRateLimiter(),
            failedAttemptCache(),
            apiUsageCache()
        );
    }
    
    /**
     * Clean up expired rate limiters periodically
     */
    @Bean
    public RateLimiterCleanupTask rateLimiterCleanupTask() {
        return new RateLimiterCleanupTask(ipRateLimiters());
    }
    
    /**
     * API Usage Statistics class
     */
    public static class APIUsageStats {
        private int totalRequests;
        private int analysisRequests;
        private long firstRequestTime;
        private long lastRequestTime;
        
        public APIUsageStats() {
            this.firstRequestTime = System.currentTimeMillis();
            this.lastRequestTime = System.currentTimeMillis();
        }
        
        public void incrementRequests() {
            this.totalRequests++;
            this.lastRequestTime = System.currentTimeMillis();
        }
        
        public void incrementAnalysisRequests() {
            this.analysisRequests++;
            this.lastRequestTime = System.currentTimeMillis();
        }
        
        // Getters
        public int getTotalRequests() { return totalRequests; }
        public int getAnalysisRequests() { return analysisRequests; }
        public long getFirstRequestTime() { return firstRequestTime; }
        public long getLastRequestTime() { return lastRequestTime; }
        
        public boolean isExpired() {
            // Consider stats expired after 10 minutes of inactivity
            return System.currentTimeMillis() - lastRequestTime > 600000;
        }
    }
}