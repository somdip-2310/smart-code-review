package com.somdiproy.smartcode.controller;

import com.somdiproy.smartcode.dto.AnalysisResponse;
import com.somdiproy.smartcode.dto.GitHubWebhookPayload;
import com.somdiproy.smartcode.service.CodeAnalysisService;
import com.somdiproy.smartcode.service.GitHubService;
import com.somdiproy.smartcode.service.SessionService;
import com.somdiproy.smartcode.util.GitHubWebhookValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * GitHub Webhook Controller
 * 
 * Handles GitHub webhook events for automated code analysis.
 * Supports push events, pull requests, and repository events.
 * 
 * @author Somdip Roy
 */
@RestController
@RequestMapping("/api/v1/github")
public class GitHubWebhookController {
    
    private static final Logger logger = LoggerFactory.getLogger(GitHubWebhookController.class);
    
    @Autowired
    private CodeAnalysisService codeAnalysisService;
    
    @Autowired
    private SessionService sessionService;
    
    @Autowired
    private GitHubService gitHubService;
    
    @Value("${github.webhook.secret:}")
    private String webhookSecret;
    
    @Value("${github.webhook.max-payload-size:5242880}") // 5MB default
    private long maxPayloadSize;
    
    /**
     * Handle incoming GitHub webhook events
     */
    @PostMapping("/webhook/{sessionToken}")
    public ResponseEntity<Map<String, Object>> handleWebhook(
            @PathVariable String sessionToken,
            @RequestHeader(value = "X-GitHub-Event", required = false) String event,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestHeader(value = "X-GitHub-Delivery", required = false) String deliveryId,
            @RequestBody String payload) {
        
        logger.info("Received GitHub webhook - Event: {}, Session: {}, Delivery: {}", 
                event, sessionToken, deliveryId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("deliveryId", deliveryId);
        
        try {
            // Validate session token
            if (!sessionService.isValidSession(sessionToken)) {
                logger.warn("Invalid session token: {}", sessionToken);
                response.put("status", "error");
                response.put("message", "Invalid or expired session");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            // Validate webhook signature if secret is configured
            if (webhookSecret != null && !webhookSecret.isEmpty()) {
                if (!validateWebhookSignature(payload, signature)) {
                    logger.warn("Invalid webhook signature for delivery: {}", deliveryId);
                    response.put("status", "error");
                    response.put("message", "Invalid webhook signature");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
                }
            }
            
            // Check payload size
            if (payload.length() > maxPayloadSize) {
                logger.warn("Payload too large: {} bytes", payload.length());
                response.put("status", "error");
                response.put("message", "Payload too large");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            // Process the webhook based on event type
            response.put("event", event);
            Map<String, Object> eventData = processWebhookEvent(event, payload, sessionToken);
            response.put("status", "success");
            response.put("data", eventData);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error processing webhook for session: " + sessionToken, e);
            response.put("status", "error");
            response.put("message", "Failed to process webhook: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Test webhook connection
     */
    @PostMapping("/webhook/test/{sessionToken}")
    public ResponseEntity<Map<String, Object>> testWebhook(
            @PathVariable String sessionToken) {
        
        logger.info("Testing webhook connection for session: {}", sessionToken);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Validate session
            if (!sessionService.isValidSession(sessionToken)) {
                response.put("status", "error");
                response.put("message", "Invalid or expired session");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            response.put("status", "success");
            response.put("message", "Webhook connection successful");
            response.put("sessionToken", sessionToken);
            response.put("timestamp", LocalDateTime.now());
            response.put("webhookUrl", generateWebhookUrl(sessionToken));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error testing webhook", e);
            response.put("status", "error");
            response.put("message", "Connection test failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Get webhook setup instructions
     */
    @GetMapping("/webhook/instructions/{sessionToken}")
    public ResponseEntity<Map<String, Object>> getWebhookInstructions(
            @PathVariable String sessionToken) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (!sessionService.isValidSession(sessionToken)) {
                response.put("status", "error");
                response.put("message", "Invalid or expired session");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            String webhookUrl = generateWebhookUrl(sessionToken);
            
            response.put("status", "success");
            response.put("webhookUrl", webhookUrl);
            response.put("instructions", Arrays.asList(
                "1. Go to your GitHub repository settings",
                "2. Click on 'Webhooks' in the left sidebar",
                "3. Click 'Add webhook'",
                "4. Paste the webhook URL: " + webhookUrl,
                "5. Set 'Content type' to 'application/json'",
                "6. Select 'Let me select individual events'",
                "7. Check 'Push events' and 'Pull requests'",
                "8. Click 'Add webhook'"
            ));
            response.put("supportedEvents", Arrays.asList("push", "pull_request", "repository"));
            
            if (webhookSecret != null && !webhookSecret.isEmpty()) {
                response.put("secretRequired", true);
                response.put("secretInstructions", "Contact support for webhook secret");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error generating webhook instructions", e);
            response.put("status", "error");
            response.put("message", "Failed to generate instructions");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Process webhook events based on type
     */
    private Map<String, Object> processWebhookEvent(String event, String payload, String sessionToken) 
            throws Exception {
        
        Map<String, Object> result = new HashMap<>();
        
        switch (event) {
            case "push":
                result = processPushEvent(payload, sessionToken);
                break;
                
            case "pull_request":
                result = processPullRequestEvent(payload, sessionToken);
                break;
                
            case "repository":
                result = processRepositoryEvent(payload, sessionToken);
                break;
                
            case "ping":
                result.put("message", "Pong! Webhook successfully connected");
                result.put("status", "active");
                break;
                
            default:
                logger.info("Unhandled event type: {}", event);
                result.put("message", "Event type not supported: " + event);
                result.put("status", "ignored");
        }
        
        return result;
    }
    
    /**
     * Process push events
     */
    private Map<String, Object> processPushEvent(String payload, String sessionToken) throws Exception {
        logger.info("Processing push event for session: {}", sessionToken);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Parse the push event payload
            Map<String, Object> pushData = gitHubService.parsePushEvent(payload);
            
            String repoName = (String) pushData.get("repository_name");
            String branch = (String) pushData.get("branch");
            List<Map<String, Object>> commits = (List<Map<String, Object>>) pushData.get("commits");
            
            result.put("repository", repoName);
            result.put("branch", branch);
            result.put("commits_count", commits.size());
            
            // Analyze changed files from the most recent commit
            if (!commits.isEmpty()) {
                Map<String, Object> latestCommit = commits.get(0);
                String commitSha = (String) latestCommit.get("id");
                List<String> modifiedFiles = (List<String>) latestCommit.get("modified");
                List<String> addedFiles = (List<String>) latestCommit.get("added");
                
                // Combine all changed files
                Set<String> changedFiles = new HashSet<>();
                if (modifiedFiles != null) changedFiles.addAll(modifiedFiles);
                if (addedFiles != null) changedFiles.addAll(addedFiles);
                
                result.put("analyzed_files", changedFiles.size());
                result.put("commit_sha", commitSha);
                
                // Trigger analysis for the changed files
                if (!changedFiles.isEmpty()) {
                    // Note: In a real implementation, you would fetch the file contents
                    // from GitHub and analyze them
                    logger.info("Analyzing {} files from commit {}", changedFiles.size(), commitSha);
                    result.put("analysis_status", "queued");
                    result.put("analysis_id", UUID.randomUUID().toString());
                }
            }
            
        } catch (Exception e) {
            logger.error("Error processing push event", e);
            result.put("error", "Failed to process push event: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Process pull request events
     */
    private Map<String, Object> processPullRequestEvent(String payload, String sessionToken) 
            throws Exception {
        
        logger.info("Processing pull request event for session: {}", sessionToken);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            Map<String, Object> prData = gitHubService.parsePullRequestEvent(payload);
            
            String action = (String) prData.get("action");
            String prNumber = String.valueOf(prData.get("number"));
            String title = (String) prData.get("title");
            
            result.put("action", action);
            result.put("pr_number", prNumber);
            result.put("title", title);
            
            // Only analyze on opened or synchronize actions
            if ("opened".equals(action) || "synchronize".equals(action)) {
                result.put("analysis_status", "queued");
                result.put("analysis_id", UUID.randomUUID().toString());
                logger.info("Queued analysis for PR #{}: {}", prNumber, title);
            } else {
                result.put("analysis_status", "skipped");
                result.put("reason", "Action not configured for analysis");
            }
            
        } catch (Exception e) {
            logger.error("Error processing pull request event", e);
            result.put("error", "Failed to process pull request: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Process repository events
     */
    private Map<String, Object> processRepositoryEvent(String payload, String sessionToken) 
            throws Exception {
        
        logger.info("Processing repository event for session: {}", sessionToken);
        
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Repository event received");
        result.put("status", "acknowledged");
        
        return result;
    }
    
    /**
     * Validate GitHub webhook signature
     */
    private boolean validateWebhookSignature(String payload, String signature) {
        if (signature == null || !signature.startsWith("sha256=")) {
            return false;
        }
        
        try {
            String expected = "sha256=" + calculateHmacSha256(payload, webhookSecret);
            return constantTimeEquals(expected, signature);
        } catch (Exception e) {
            logger.error("Error validating webhook signature", e);
            return false;
        }
    }
    
    /**
     * Calculate HMAC SHA256
     */
    private String calculateHmacSha256(String data, String secret) 
            throws NoSuchAlgorithmException, InvalidKeyException {
        
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(
            secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"
        );
        mac.init(secretKeySpec);
        
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        
        // Convert to hex string
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        
        return hexString.toString();
    }
    
    /**
     * Constant time string comparison to prevent timing attacks
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        
        return result == 0;
    }
    
    /**
     * Generate webhook URL for a session
     */
    private String generateWebhookUrl(String sessionToken) {
        // In production, this should use the actual domain from configuration
        String baseUrl = "https://smartcode.somdip.dev";
        return baseUrl + "/api/v1/github/webhook/" + sessionToken;
    }
}