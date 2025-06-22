package com.somdiproy.smartcode.interceptor;

import com.github.benmanes.caffeine.cache.Cache;
import com.google.common.util.concurrent.RateLimiter;
import com.somdiproy.smartcode.config.RateLimitConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Rate Limiting Interceptor
 * 
 * Enforces rate limits on API endpoints
 * 
 * @author Somdip Roy
 */
public class RateLimitInterceptor implements HandlerInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimitInterceptor.class);
    
    private final RateLimiter sessionCreationRateLimiter;
    private final RateLimiter apiRateLimiter;
    private final RateLimiter analysisRateLimiter;
    private final Cache<String, Integer> failedAttemptCache;
    private final Cache<String, RateLimitConfig.APIUsageStats> apiUsageCache;
    
    public RateLimitInterceptor(RateLimiter sessionCreationRateLimiter,
                               RateLimiter apiRateLimiter,
                               RateLimiter analysisRateLimiter,
                               Cache<String, Integer> failedAttemptCache,
                               Cache<String, RateLimitConfig.APIUsageStats> apiUsageCache) {
        this.sessionCreationRateLimiter = sessionCreationRateLimiter;
        this.apiRateLimiter = apiRateLimiter;
        this.analysisRateLimiter = analysisRateLimiter;
        this.failedAttemptCache = failedAttemptCache;
        this.apiUsageCache = apiUsageCache;
    }
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        String clientIp = getClientIp(request);
        
        // Check rate limits based on endpoint
        boolean allowed = true;
        
        if (path.contains("/session/create")) {
            allowed = sessionCreationRateLimiter.tryAcquire();
            if (!allowed) {
                logger.warn("Session creation rate limit exceeded for IP: {}", clientIp);
            }
        } else if (path.contains("/analyze")) {
            allowed = analysisRateLimiter.tryAcquire();
            if (!allowed) {
                logger.warn("Analysis rate limit exceeded for IP: {}", clientIp);
            }
        } else {
            allowed = apiRateLimiter.tryAcquire();
            if (!allowed) {
                logger.warn("API rate limit exceeded for IP: {}", clientIp);
            }
        }
        
        if (!allowed) {
            response.setStatus(HttpServletResponse.SC_TOO_MANY_REQUESTS);
            response.setHeader("Retry-After", "60");
            response.getWriter().write("{\"error\":\"Rate limit exceeded. Please try again later.\"}");
            return false;
        }
        
        // Track API usage
        String sessionToken = extractSessionToken(request);
        if (sessionToken != null) {
            RateLimitConfig.APIUsageStats stats = apiUsageCache.get(sessionToken, k -> new RateLimitConfig.APIUsageStats());
            stats.incrementRequests();
            if (path.contains("/analyze")) {
                stats.incrementAnalysisRequests();
            }
            apiUsageCache.put(sessionToken, stats);
        }
        
        return true;
    }
    
    private String getClientIp(HttpServletRequest request) {
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
    
    private String extractSessionToken(HttpServletRequest request) {
        // Extract from header
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        
        // Extract from query parameter
        return request.getParameter("sessionToken");
    }
}