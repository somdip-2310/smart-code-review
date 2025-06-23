package com.somdiproy.smartcode.config;

import com.google.common.util.concurrent.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Scheduled task to clean up expired IP rate limiters
 * 
 * @author Somdip Roy
 */
public class RateLimiterCleanupTask {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimiterCleanupTask.class);
    
    private final ConcurrentHashMap<String, RateLimiter> ipRateLimiters;
    
    // Track last access time for each IP
    private final ConcurrentHashMap<String, Long> lastAccessTime = new ConcurrentHashMap<>();
    
    // Expire rate limiters after 1 hour of inactivity
    private static final long EXPIRY_TIME_MS = 60 * 60 * 1000; // 1 hour
    
    public RateLimiterCleanupTask(ConcurrentHashMap<String, RateLimiter> ipRateLimiters) {
        this.ipRateLimiters = ipRateLimiters;
    }
    
    /**
     * Clean up expired rate limiters every 15 minutes
     */
    @Scheduled(fixedDelay = 900000) // 15 minutes
    public void cleanup() {
        logger.debug("Starting rate limiter cleanup task");
        
        int removedCount = 0;
        long currentTime = System.currentTimeMillis();
        
        Iterator<Map.Entry<String, Long>> iterator = lastAccessTime.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            String ipAddress = entry.getKey();
            Long lastAccess = entry.getValue();
            
            if (currentTime - lastAccess > EXPIRY_TIME_MS) {
                // Remove rate limiter for this IP
                ipRateLimiters.remove(ipAddress);
                iterator.remove();
                removedCount++;
                logger.debug("Removed rate limiter for IP: {}", ipAddress);
            }
        }
        
        if (removedCount > 0) {
            logger.info("Rate limiter cleanup completed. Removed {} expired limiters. Active limiters: {}", 
                       removedCount, ipRateLimiters.size());
        }
    }
    
    /**
     * Update last access time for an IP address
     * This should be called whenever a rate limiter is accessed
     */
    public void updateLastAccess(String ipAddress) {
        lastAccessTime.put(ipAddress, System.currentTimeMillis());
    }
    
    /**
     * Get current number of active rate limiters
     */
    public int getActiveRateLimitersCount() {
        return ipRateLimiters.size();
    }
}