package com.somdiproy.smartcode.controller;

import com.somdiproy.smartcode.dto.AnalysisRequest;
import com.somdiproy.smartcode.dto.AnalysisResponse;
import com.somdiproy.smartcode.dto.AnalysisType;
import com.somdiproy.smartcode.dto.GitHubWebhookPayload;
import com.somdiproy.smartcode.service.CodeAnalysisService;
import com.somdiproy.smartcode.service.GitHubService;
import com.somdiproy.smartcode.service.SessionService;
import com.somdiproy.smartcode.service.SessionService.SessionData;
import com.somdiproy.smartcode.util.GitHubWebhookValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
     * Track webhook events for each session
     */
    private final Map<String, WebhookStatus> webhookStatusMap = new ConcurrentHashMap<>();
    
    /**
     * Get webhook connection status
     */
    @GetMapping("/webhook/status/{sessionToken}")
    public ResponseEntity<Map<String, Object>> getWebhookStatus(@PathVariable String sessionToken) {
        logger.info("Checking webhook status for session: {}", sessionToken);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Validate session token
            if (!sessionService.isValidSession(sessionToken)) {
                response.put("connected", false);
                response.put("sessionValid", false);
                response.put("message", "Invalid or expired session");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            // Check if we've received any webhook events for this session
            WebhookStatus status = webhookStatusMap.get(sessionToken);
            
            if (status != null) {
                response.put("connected", true);
                response.put("sessionValid", true);
                response.put("lastPingReceived", status.getLastPingTime());
                response.put("totalEventsReceived", status.getTotalEvents());
                response.put("lastEventType", status.getLastEventType());
                response.put("message", "Webhook is connected and active");
                
                // Add session info
                SessionData sessionData = sessionService.getSessionByToken(sessionToken);
                if (sessionData != null) {
                    long remainingTime = sessionService.getRemainingTime(sessionToken);
                    response.put("remainingMinutes", remainingTime / 60000);
                }
            } else {
                response.put("connected", false);
                response.put("sessionValid", true);
                response.put("message", "No webhook events received yet. Please configure the webhook in GitHub.");
                response.put("webhookUrl", getWebhookUrl(sessionToken));
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error checking webhook status", e);
            response.put("connected", false);
            response.put("sessionValid", false);
            response.put("message", "Error checking webhook status");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
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
                logger.warn("Invalid session token received in webhook: {}", sessionToken);
                response.put("status", "error");
                response.put("message", "Invalid or expired session");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            // Track webhook event
            trackWebhookEvent(sessionToken, event);
            
            // Get session data for additional validation and logging
            SessionService.SessionData sessionData = sessionService.getSessionByToken(sessionToken);
            if (sessionData == null) {
                logger.error("Session validation passed but data not found for token: {}", sessionToken);
                response.put("status", "error");
                response.put("message", "Session data corrupted");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
            
            String userEmail = sessionData.getEmail();
            logger.info("Valid webhook received for session: {} (user: {})", 
                        sessionToken, maskEmail(userEmail));
            
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
            
            // Check if this is a ping event
            if ("ping".equals(event)) {
                logger.info("Received ping event from GitHub");
                response.put("status", "success");
                response.put("message", "Pong! Webhook successfully connected");
                response.put("event", "ping");
                
                // Update webhook status for ping
                WebhookStatus status = webhookStatusMap.get(sessionToken);
                if (status != null) {
                    status.setLastPingTime(System.currentTimeMillis());
                }
                
                return ResponseEntity.ok(response);
            }
            
            // Check if we support this event type
            List<String> supportedEvents = Arrays.asList("push", "pull_request", "repository");
            if (event != null && !supportedEvents.contains(event)) {
                logger.info("Unsupported event type: {}", event);
                response.put("status", "ignored");
                response.put("message", "Event type not supported: " + event);
                response.put("supportedEvents", supportedEvents);
                return ResponseEntity.ok(response);
            }
            
            // Process the webhook based on event type
            response.put("event", event);
            response.put("userEmail", maskEmail(userEmail));
            
            Map<String, Object> eventData = processWebhookEvent(event, payload, sessionToken);
            
            // Check if processing was successful
            if (eventData.containsKey("error")) {
                response.put("status", "error");
                response.put("message", eventData.get("error"));
                response.put("data", eventData);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
            
            response.put("status", "success");
            response.put("data", eventData);
            
            // Log successful processing
            logger.info("Successfully processed {} event for session: {} (analysis_id: {})", 
                        event, sessionToken, eventData.get("analysis_id"));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error processing webhook for session: " + sessionToken, e);
            response.put("status", "error");
            response.put("message", "Failed to process webhook: " + e.getMessage());
            response.put("errorType", e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Track webhook events for status monitoring
     */
    private void trackWebhookEvent(String sessionToken, String eventType) {
        WebhookStatus status = webhookStatusMap.computeIfAbsent(sessionToken, 
            k -> new WebhookStatus());
        
        status.setLastEventTime(System.currentTimeMillis());
        status.setLastEventType(eventType);
        status.incrementTotalEvents();
        
        if ("ping".equals(eventType)) {
            status.setLastPingTime(System.currentTimeMillis());
        }
        
        logger.debug("Updated webhook status for session {}: {} events received", 
            sessionToken, status.getTotalEvents());
    }
    
    /**
     * Clean up webhook status for expired sessions
     */
    @Scheduled(fixedDelay = 300000) // Every 5 minutes
    public void cleanupWebhookStatus() {
        webhookStatusMap.entrySet().removeIf(entry -> 
            !sessionService.isValidSession(entry.getKey())
        );
    }

    /**
     * Process webhook events based on type
     */
    private Map<String, Object> processWebhookEvent(String event, String payload, String sessionToken) 
            throws Exception {
        
        Map<String, Object> result = new HashMap<>();
        
        if (event == null) {
            result.put("error", "No event type specified");
            return result;
        }
        
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
     * Validate GitHub webhook signature
     */
    private boolean validateWebhookSignature(String payload, String signature) {
        if (signature == null || !signature.startsWith("sha256=")) {
            return false;
        }
        
        try {
            String expected = "sha256=" + calculateHmacSha256(payload, webhookSecret);
            return MessageDigest.isEqual(expected.getBytes(), signature.getBytes());
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
            secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        
        // Convert to hex string
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
    
    /**
     * Helper method to mask email
     */
    private String maskEmail(String email) {
        if (email == null || email.length() < 3) return "***";
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) return "***";
        
        String username = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        
        if (username.length() <= 2) {
            return username.charAt(0) + "*" + domain;
        } else {
            return username.charAt(0) + 
                   "*".repeat(Math.min(username.length() - 2, 3)) + 
                   username.charAt(username.length() - 1) + 
                   domain;
        }
    }
    
    /**
     * Test webhook endpoint
     */
    @PostMapping("/webhook/test/{sessionToken}")
    public ResponseEntity<Map<String, Object>> testWebhook(@PathVariable String sessionToken) {
        logger.info("Testing webhook connection for session: {}", sessionToken);
        
        Map<String, Object> response = new HashMap<>();
        response.put("sessionToken", sessionToken);
        
        try {
            // CRITICAL: Validate the session token
            if (!sessionService.isValidSession(sessionToken)) {
                logger.warn("Invalid session token for webhook test: {}", sessionToken);
                response.put("status", "error");
                response.put("message", "Invalid or expired session token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            // Get session details to confirm it exists
            String userEmail = sessionService.getSessionEmail(sessionToken);
            if (userEmail == null) {
                logger.warn("Session exists but no email found for token: {}", sessionToken);
                response.put("status", "error");
                response.put("message", "Session is invalid or corrupted");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            logger.info("Webhook test successful for session: {} (user: {})", 
                        sessionToken, userEmail.replaceAll("(?<=.{1}).(?=.*@)", "*"));
            
            response.put("message", "Webhook connection successful");
            response.put("status", "success");
            response.put("webhookUrl", getWebhookUrl(sessionToken));
            response.put("timestamp", System.currentTimeMillis());
            
            // Add remaining time info
            long remainingTime = sessionService.getRemainingTime(sessionToken);
            response.put("remainingMinutes", remainingTime / 60000);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error testing webhook", e);
            response.put("status", "error");
            response.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Helper method to generate webhook URL
     */
    private String getWebhookUrl(String sessionToken) {
        // Use the actual domain from configuration or request
        return String.format("https://smartcode.somdip.dev/api/v1/github/webhook/%s", sessionToken);
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
                    repoName = (String) pushData.get("repository_name");
                    
                    // Fetch file contents from GitHub
                    Map<String, String> fileContents = new HashMap<>();
                    for (String filePath : changedFiles) {
                        String content = gitHubService.fetchFileContent(repoName, filePath, commitSha);
                        if (content != null && isCodeFile(filePath)) {
                            fileContents.put(filePath, content);
                        }
                    }
                    
                    if (!fileContents.isEmpty()) {
                        // Prepare combined code for analysis
                        StringBuilder combinedCode = new StringBuilder();
                        List<String> fileNames = new ArrayList<>();
                        
                        for (Map.Entry<String, String> entry : fileContents.entrySet()) {
                            combinedCode.append("// File: ").append(entry.getKey()).append("\n");
                            combinedCode.append(entry.getValue()).append("\n\n");
                            fileNames.add(entry.getKey());
                        }
                        
                        // Create analysis request using correct field names
                        AnalysisRequest analysisRequest = AnalysisRequest.builder()
                            .type(AnalysisType.GITHUB_WEBHOOK)
                            .sessionToken(sessionToken)  // Note: it's sessionToken, not sessionId
                            .fileName(String.join(",", fileNames))
                            .language("auto-detect")
                            .build();
                        
                        // Trigger the actual analysis
                        AnalysisResponse analysisResponse = codeAnalysisService.analyzeCode(
                            combinedCode.toString(), analysisRequest
                        );
                        
                        result.put("analysis_status", "started");
                        result.put("analysis_id", analysisResponse.getAnalysisId());
                        result.put("analysis_message", analysisResponse.getMessage());
                        
                        logger.info("Started analysis {} for {} files from commit {}", 
                            analysisResponse.getAnalysisId(), fileContents.size(), commitSha);
                            
                        // Optional: Create GitHub check run
                        gitHubService.createCheckRun(repoName, commitSha, 
                            "Smart Code Review", "in_progress", null, 
                            "Analyzing " + fileContents.size() + " files...");
                    } else {
                        result.put("analysis_status", "skipped");
                        result.put("reason", "No code files found to analyze");
                    }
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
                String repoName = (String) prData.get("repository_name");
                String headSha = (String) prData.get("head_sha");
                int prNumberInt = Integer.parseInt(prNumber);
                
                // Use the correct method name: getPullRequestFiles
                List<Map<String, String>> prFiles = gitHubService.getPullRequestFiles(
                    repoName, prNumberInt
                );
                
                // Fetch file contents
                Map<String, String> fileContents = new HashMap<>();
                for (Map<String, String> fileInfo : prFiles) {
                    String filePath = fileInfo.get("filename");
                    if (isCodeFile(filePath)) {
                        String content = gitHubService.fetchFileContent(repoName, filePath, headSha);
                        if (content != null) {
                            fileContents.put(filePath, content);
                        }
                    }
                }
                
                if (!fileContents.isEmpty()) {
                    // Prepare combined code for analysis
                    StringBuilder combinedCode = new StringBuilder();
                    List<String> fileNames = new ArrayList<>();
                    
                    for (Map.Entry<String, String> entry : fileContents.entrySet()) {
                        combinedCode.append("// File: ").append(entry.getKey()).append("\n");
                        combinedCode.append(entry.getValue()).append("\n\n");
                        fileNames.add(entry.getKey());
                    }
                    
                    // Create analysis request with correct field names
                    AnalysisRequest analysisRequest = AnalysisRequest.builder()
                        .type(AnalysisType.GITHUB_WEBHOOK)
                        .sessionToken(sessionToken)  // Using sessionToken, not sessionId
                        .fileName("PR #" + prNumber + ": " + String.join(",", fileNames))
                        .language("auto-detect")
                        .build();
                    
                    AnalysisResponse analysisResponse = codeAnalysisService.analyzeCode(
                        combinedCode.toString(), analysisRequest
                    );
                    
                    result.put("analysis_status", "started");
                    result.put("analysis_id", analysisResponse.getAnalysisId());
                    
                    // Create GitHub check run for PR
                    gitHubService.createCheckRun(repoName, headSha,
                        "Smart Code Review", "in_progress", null,
                        "Analyzing PR #" + prNumber + " (" + fileContents.size() + " files)");
                }
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
     * Check if file is a code file based on extension
     */
    private boolean isCodeFile(String fileName) {
        String[] codeExtensions = {
            ".java", ".py", ".js", ".ts", ".cpp", ".c", ".cs", ".go", 
            ".rb", ".php", ".swift", ".kt", ".rs", ".scala", ".html", 
            ".css", ".xml", ".json", ".yaml", ".yml", ".sql"
        };
        
        String lowerFileName = fileName.toLowerCase();
        for (String ext : codeExtensions) {
            if (lowerFileName.endsWith(ext)) {
                return true;
            }
        }
        return false;
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
    
    /**
     * Inner class to track webhook status
     */
    private static class WebhookStatus {
        private long lastEventTime;
        private long lastPingTime;
        private String lastEventType;
        private int totalEvents;
        
        public long getLastEventTime() {
            return lastEventTime;
        }
        
        public void setLastEventTime(long lastEventTime) {
            this.lastEventTime = lastEventTime;
        }
        
        public long getLastPingTime() {
            return lastPingTime;
        }
        
        public void setLastPingTime(long lastPingTime) {
            this.lastPingTime = lastPingTime;
        }
        
        public String getLastEventType() {
            return lastEventType;
        }
        
        public void setLastEventType(String lastEventType) {
            this.lastEventType = lastEventType;
        }
        
        public int getTotalEvents() {
            return totalEvents;
        }
        
        public void incrementTotalEvents() {
            this.totalEvents++;
        }
    }
}