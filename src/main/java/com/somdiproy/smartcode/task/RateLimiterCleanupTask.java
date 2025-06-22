package com.somdiproy.smartcode.task;

import com.google.common.util.concurrent.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limiter Cleanup Task
 * 
 * Periodically cleans up expired rate limiters to prevent memory leaks
 * 
 * @author Somdip Roy
 */
@Component
public class RateLimiterCleanupTask {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimiterCleanupTask.class);
    
    private final ConcurrentHashMap<String, RateLimiter> ipRateLimiters;
    private final ConcurrentHashMap<String, Long> lastAccessTime = new ConcurrentHashMap<>();
    
    private static final long EXPIRY_TIME_MS = 3600000; // 1 hour
    
    public RateLimiterCleanupTask(ConcurrentHashMap<String, RateLimiter> ipRateLimiters) {
        this.ipRateLimiters = ipRateLimiters;
    }
    
    @Scheduled(fixedDelay = 300000) // Run every 5 minutes
    public void cleanup() {
        logger.debug("Starting rate limiter cleanup");
        
        long currentTime = System.currentTimeMillis();
        int cleaned = 0;
        
        Iterator<Map.Entry<String, Long>> iterator = lastAccessTime.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            String ip = entry.getKey();
            Long lastAccess = entry.getValue();
            
            if (currentTime - lastAccess > EXPIRY_TIME_MS) {
                ipRateLimiters.remove(ip);
                iterator.remove();
                cleaned++;
            }
        }
        
        if (cleaned > 0) {
            logger.info("Cleaned up {} expired rate limiters", cleaned);
        }
    }
    
    public void updateLastAccess(String ip) {
        lastAccessTime.put(ip, System.currentTimeMillis());
    }
}