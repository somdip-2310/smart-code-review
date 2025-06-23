package com.somdiproy.smartcode.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.google.common.util.concurrent.RateLimiter;
import com.somdiproy.smartcode.config.RateLimitConfig.APIUsageStats;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;

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
    }
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestPath = request.getRequestURI();
        String sessionToken = extractSessionToken(request);
        String ipAddress = getClientIpAddress(request);
        
        logger.debug("Rate limit check for path: {}, IP: {}", requestPath, ipAddress);
        
        // Check different rate limits based on endpoint
        if (requestPath.contains("/session/create")) {
            if (!sessionCreationRateLimiter.tryAcquire()) {
                logger.warn("Session creation rate limit exceeded for IP: {}", ipAddress);
                response.setStatus(SC_TOO_MANY_REQUESTS);
                response.getWriter().write("{\"error\":\"Too many session creation requests. Please try again later.\"}");
                return false;
            }
        } else if (requestPath.contains("/analysis")) {
            if (!analysisRateLimiter.tryAcquire()) {
                logger.warn("Analysis rate limit exceeded for session: {}", sessionToken);
                response.setStatus(SC_TOO_MANY_REQUESTS);
                response.getWriter().write("{\"error\":\"Too many analysis requests. Please try again later.\"}");
                return false;
            }
            // Update usage stats
            updateAPIUsageStats(sessionToken, true);
        } else {
            if (!apiRateLimiter.tryAcquire()) {
                logger.warn("API rate limit exceeded for session: {}", sessionToken);
                response.setStatus(SC_TOO_MANY_REQUESTS);
                response.getWriter().write("{\"error\":\"Too many API requests. Please try again later.\"}");
                return false;
            }
            // Update usage stats
            updateAPIUsageStats(sessionToken, false);
        }
        
        return true;
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