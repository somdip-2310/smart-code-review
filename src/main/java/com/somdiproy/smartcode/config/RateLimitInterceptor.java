package com.somdiproy.smartcode.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.google.common.util.concurrent.RateLimiter;
import com.somdiproy.smartcode.config.RateLimitConfig.APIUsageStats;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * Rate Limiting Interceptor for API endpoints
 * 
 * @author Somdip Roy
 */
public class RateLimitInterceptor implements HandlerInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimitInterceptor.class);
    
    private static final int SC_TOO_MANY_REQUESTS = 429;
    private final RateLimiter sessionCreationRateLimiter;
    private final RateLimiter apiRateLimiter;
    private final RateLimiter analysisRateLimiter;
    private final Cache<String, Integer> failedAttemptCache;
    private final Cache<String, APIUsageStats> apiUsageCache;
    
    // Store rate limits for headers
    private final double apiCallsPerMinute;
    private final double analysisCallsPerMinute;
    
    public RateLimitInterceptor(RateLimiter sessionCreationRateLimiter,
                                RateLimiter apiRateLimiter,
                                RateLimiter analysisRateLimiter,
                                Cache<String, Integer> failedAttemptCache,
                                Cache<String, APIUsageStats> apiUsageCache) {
        this.sessionCreationRateLimiter = sessionCreationRateLimiter;
        this.apiRateLimiter = apiRateLimiter;
        this.analysisRateLimiter = analysisRateLimiter;
        this.failedAttemptCache = failedAttemptCache;
        this.apiUsageCache = apiUsageCache;
        
        // Store rate limits (assuming 60 and 5 per minute based on logs)
        this.apiCallsPerMinute = 60.0;
        this.analysisCallsPerMinute = 20.0; // Increased from 5 to 20
    }
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestPath = request.getRequestURI();
        String sessionToken = extractSessionToken(request);
        String ipAddress = getClientIpAddress(request);
        String method = request.getMethod();
        
        logger.debug("Rate limit check for path: {}, IP: {}, Method: {}", requestPath, ipAddress, method);
        
        // Set CORS headers for error responses
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        
        // Check different rate limits based on endpoint
        if (requestPath.contains("/session/create")) {
            if (!sessionCreationRateLimiter.tryAcquire()) {
                logger.warn("Session creation rate limit exceeded for IP: {}", ipAddress);
                response.setStatus(SC_TOO_MANY_REQUESTS);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Too many session creation requests. Please try again later.\"}");
                return false;
            }
        } else if (requestPath.contains("/analyze/") && method.equals("POST")) {
            // This is for submitting new analysis (POST requests)
            if (!analysisRateLimiter.tryAcquire()) {
                logger.warn("Analysis submission rate limit exceeded for session: {}", sessionToken);
                response.setStatus(SC_TOO_MANY_REQUESTS);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Too many analysis submissions. Please try again later.\"}");
                return false;
            }
            updateAPIUsageStats(sessionToken, true);
        } else if (requestPath.contains("/analysis/") && method.equals("GET")) {
            // For polling analysis results (GET requests), use a more lenient rate limit
            // Use API rate limiter which has higher limits
            if (!apiRateLimiter.tryAcquire()) {
                logger.warn("API rate limit exceeded for analysis polling, session: {}", sessionToken);
                response.setStatus(SC_TOO_MANY_REQUESTS);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Too many requests. Please wait a moment before retrying.\"}");
                return false;
            }
            updateAPIUsageStats(sessionToken, false);
        } else {
            // All other API endpoints
            if (!apiRateLimiter.tryAcquire()) {
                logger.warn("API rate limit exceeded for session: {}", sessionToken);
                response.setStatus(SC_TOO_MANY_REQUESTS);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Too many API requests. Please try again later.\"}");
                return false;
            }
            updateAPIUsageStats(sessionToken, false);
        }
        
        return true;
    }
    
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, 
                          Object handler, ModelAndView modelAndView) throws Exception {
        String requestPath = request.getRequestURI();
        
        // Add rate limit headers to response
        if (requestPath.contains("/analysis/") && request.getMethod().equals("GET")) {
            // For analysis polling, show API rate limits
            response.setHeader("X-RateLimit-Limit", String.valueOf((int)apiCallsPerMinute));
            response.setHeader("X-RateLimit-Window", "60s");
        } else if (requestPath.contains("/analyze/")) {
            // For analysis submission, show analysis rate limits
            response.setHeader("X-RateLimit-Limit", String.valueOf((int)analysisCallsPerMinute));
            response.setHeader("X-RateLimit-Window", "60s");
        } else {
            // For other endpoints, show API rate limits
            response.setHeader("X-RateLimit-Limit", String.valueOf((int)apiCallsPerMinute));
            response.setHeader("X-RateLimit-Window", "60s");
        }
        
        // Add retry-after header for rate limited responses
        if (response.getStatus() == SC_TOO_MANY_REQUESTS) {
            response.setHeader("Retry-After", "10"); // Suggest retry after 10 seconds
        }
    }
    
    private String extractSessionToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return request.getParameter("sessionToken");
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
    
    private void updateAPIUsageStats(String sessionToken, boolean isAnalysis) {
        if (sessionToken == null) return;
        
        APIUsageStats stats = apiUsageCache.get(sessionToken, k -> new APIUsageStats());
        stats.incrementRequests();
        if (isAnalysis) {
            stats.incrementAnalysisRequests();
        }
        apiUsageCache.put(sessionToken, stats);
    }
}