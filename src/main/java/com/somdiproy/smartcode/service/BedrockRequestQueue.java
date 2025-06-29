package com.somdiproy.smartcode.service;

import com.google.common.util.concurrent.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class BedrockRequestQueue {
    private static final Logger logger = LoggerFactory.getLogger(BedrockRequestQueue.class);
    
    @Value("${aws.bedrock.max-concurrent-requests:2}")
    private int maxConcurrentRequests;
    
    @Value("${aws.bedrock.requests-per-minute:10}")
    private double requestsPerMinute;
    
    private ExecutorService executorService;
    private RateLimiter rateLimiter;
    private Semaphore concurrencyLimiter;
    private final AtomicInteger activeRequests = new AtomicInteger(0);
    
    @PostConstruct
    public void init() {
        this.executorService = Executors.newFixedThreadPool(maxConcurrentRequests);
        // Ultra-conservative rate limiting - 1 request per minute
        this.rateLimiter = RateLimiter.create(1.0 / 60.0);
        this.concurrencyLimiter = new Semaphore(1); // Reduce concurrency
        logger.info("BedrockRequestQueue initialized with maxConcurrent={}, requestsPerMinute={}", 
                    maxConcurrentRequests / 2, requestsPerMinute);
    }
    
    public <T> CompletableFuture<T> submitRequest(Callable<T> task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Wait for rate limiter
                rateLimiter.acquire();
                
                // Wait for concurrency slot
                concurrencyLimiter.acquire();
                int active = activeRequests.incrementAndGet();
                logger.debug("Executing Bedrock request. Active requests: {}", active);
                
                try {
                    return task.call();
                } finally {
                    activeRequests.decrementAndGet();
                    concurrencyLimiter.release();
                }
            } catch (Exception e) {
                logger.error("Error executing Bedrock request", e);
                throw new RuntimeException("Failed to execute Bedrock request", e);
            }
        }, executorService);
    }
    
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}