package com.somdiproy.smartcode.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class BedrockCircuitBreaker {
    private static final Logger logger = LoggerFactory.getLogger(BedrockCircuitBreaker.class);
    
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0);
    private final AtomicLong circuitOpenTime = new AtomicLong(0);
    
    private static final int FAILURE_THRESHOLD = 5;
    private static final long CIRCUIT_OPEN_DURATION = 60000; // 1 minute
    private static final long FAILURE_WINDOW = 300000; // 5 minutes
    
    public boolean allowRequest() {
        long now = System.currentTimeMillis();
        
        // Check if circuit is open
        if (circuitOpenTime.get() > 0) {
            if (now - circuitOpenTime.get() < CIRCUIT_OPEN_DURATION) {
                logger.warn("Circuit breaker is OPEN. Rejecting request.");
                return false;
            } else {
                // Circuit cooldown period has passed, reset
                logger.info("Circuit breaker cooldown complete. Resetting to CLOSED.");
                reset();
            }
        }
        
        // Reset failure count if outside failure window
        if (now - lastFailureTime.get() > FAILURE_WINDOW) {
            failureCount.set(0);
        }
        
        return true;
    }
    
    public void recordSuccess() {
        // Reset on success
        failureCount.set(0);
        circuitOpenTime.set(0);
    }
    
    public void recordFailure() {
        lastFailureTime.set(System.currentTimeMillis());
        int failures = failureCount.incrementAndGet();
        
        if (failures >= FAILURE_THRESHOLD) {
            circuitOpenTime.set(System.currentTimeMillis());
            logger.error("Circuit breaker threshold reached. Opening circuit for {} ms", CIRCUIT_OPEN_DURATION);
        }
    }
    
    private void reset() {
        failureCount.set(0);
        lastFailureTime.set(0);
        circuitOpenTime.set(0);
    }
}