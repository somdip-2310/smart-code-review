package com.somdiproy.smartcode.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.somdiproy.smartcode.dto.GitHubWebhookPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * GitHub Integration Service
 * 
 * Handles GitHub API interactions and webhook payload processing
 * 
 * @author Somdip Roy
 */
@Service
public class GitHubService {
    
    private static final Logger logger = LoggerFactory.getLogger(GitHubService.class);
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${github.api.base-url:https://api.github.com}")
    private String githubApiBaseUrl;
    
    @Value("${github.api.token:}")
    private String githubApiToken;
    
    /**
     * Parse push event payload
     */
    public Map<String, Object> parsePushEvent(String payload) throws IOException {
        logger.debug("Parsing push event payload");
        
        JsonNode root = objectMapper.readTree(payload);
        Map<String, Object> result = new HashMap<>();
        
        // Extract repository information
        JsonNode repository = root.path("repository");
        result.put("repository_name", repository.path("full_name").asText());
        result.put("repository_url", repository.path("html_url").asText());
        result.put("repository_id", repository.path("id").asLong());
        
        // Extract branch information
        String ref = root.path("ref").asText();
        String branch = ref.startsWith("refs/heads/") ? ref.substring(11) : ref;
        result.put("branch", branch);
        result.put("before", root.path("before").asText());
        result.put("after", root.path("after").asText());
        
        // Extract pusher information
        JsonNode pusher = root.path("pusher");
        result.put("pusher_name", pusher.path("name").asText());
        result.put("pusher_email", pusher.path("email").asText());
        
        // Extract commits
        List<Map<String, Object>> commits = new ArrayList<>();
        JsonNode commitsNode = root.path("commits");
        
        for (JsonNode commitNode : commitsNode) {
            Map<String, Object> commit = new HashMap<>();
            commit.put("id", commitNode.path("id").asText());
            commit.put("message", commitNode.path("message").asText());
            commit.put("timestamp", commitNode.path("timestamp").asText());
            commit.put("url", commitNode.path("url").asText());
            
            // Extract file changes
            commit.put("added", extractStringList(commitNode.path("added")));
            commit.put("modified", extractStringList(commitNode.path("modified")));
            commit.put("removed", extractStringList(commitNode.path("removed")));
            
            // Author information
            JsonNode author = commitNode.path("author");
            commit.put("author_name", author.path("name").asText());
            commit.put("author_email", author.path("email").asText());
            
            commits.add(commit);
        }
        
        result.put("commits", commits);
        result.put("total_commits", commits.size());
        
        logger.info("Parsed push event: {} commits to {} on branch {}", 
            commits.size(), result.get("repository_name"), branch);
        
        return result;
    }
    
    /**
     * Parse pull request event payload
     */
    public Map<String, Object> parsePullRequestEvent(String payload) throws IOException {
        logger.debug("Parsing pull request event payload");
        
        JsonNode root = objectMapper.readTree(payload);
        Map<String, Object> result = new HashMap<>();
        
        // Extract action
        result.put("action", root.path("action").asText());
        
        // Extract pull request information
        JsonNode pullRequest = root.path("pull_request");
        result.put("number", pullRequest.path("number").asInt());
        result.put("title", pullRequest.path("title").asText());
        result.put("body", pullRequest.path("body").asText());
        result.put("state", pullRequest.path("state").asText());
        result.put("html_url", pullRequest.path("html_url").asText());
        result.put("created_at", pullRequest.path("created_at").asText());
        result.put("updated_at", pullRequest.path("updated_at").asText());
        
        // Extract head (source) branch information
        JsonNode head = pullRequest.path("head");
        result.put("head_ref", head.path("ref").asText());
        result.put("head_sha", head.path("sha").asText());
        
        // Extract base (target) branch information
        JsonNode base = pullRequest.path("base");
        result.put("base_ref", base.path("ref").asText());
        result.put("base_sha", base.path("sha").asText());
        
        // Extract user information
        JsonNode user = pullRequest.path("user");
        result.put("user_login", user.path("login").asText());
        result.put("user_type", user.path("type").asText());
        
        // Extract repository information
        JsonNode repository = root.path("repository");
        result.put("repository_name", repository.path("full_name").asText());
        result.put("repository_url", repository.path("html_url").asText());
        
        // Files changed (if available)
        result.put("changed_files", pullRequest.path("changed_files").asInt());
        result.put("additions", pullRequest.path("additions").asInt());
        result.put("deletions", pullRequest.path("deletions").asInt());
        
        logger.info("Parsed PR event: {} - PR #{} in {}", 
            result.get("action"), result.get("number"), result.get("repository_name"));
        
        return result;
    }
    
    
    /**
     * Get list of changed files in a pull request
     */
    public List<Map<String, String>> getPullRequestFiles(String repoFullName, int prNumber) {
        try {
            String url = String.format("%s/repos/%s/pulls/%d/files", 
                githubApiBaseUrl, repoFullName, prNumber);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/vnd.github.v3+json");
            if (!githubApiToken.isEmpty()) {
                headers.set("Authorization", "Bearer " + githubApiToken);
            }
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode files = objectMapper.readTree(response.getBody());
                List<Map<String, String>> fileList = new ArrayList<>();
                
                for (JsonNode file : files) {
                    Map<String, String> fileInfo = new HashMap<>();
                    fileInfo.put("filename", file.path("filename").asText());
                    fileInfo.put("status", file.path("status").asText());
                    fileInfo.put("additions", String.valueOf(file.path("additions").asInt()));
                    fileInfo.put("deletions", String.valueOf(file.path("deletions").asInt()));
                    fileInfo.put("changes", String.valueOf(file.path("changes").asInt()));
                    fileList.add(fileInfo);
                }
                
                return fileList;
            }
            
        } catch (Exception e) {
            logger.error("Error fetching PR files from GitHub: {}", e.getMessage());
        }
        
        return Collections.emptyList();
    }
    
    /**
     * Create a GitHub check run for analysis results
     */
    public void createCheckRun(String repoFullName, String headSha, 
                              String name, String status, String conclusion, 
                              String summary) {
        try {
            String url = String.format("%s/repos/%s/check-runs", 
                githubApiBaseUrl, repoFullName);
            
            Map<String, Object> checkRun = new HashMap<>();
            checkRun.put("name", name);
            checkRun.put("head_sha", headSha);
            checkRun.put("status", status);
            if (conclusion != null) {
                checkRun.put("conclusion", conclusion);
            }
            checkRun.put("started_at", LocalDateTime.now().toString());
            if ("completed".equals(status)) {
                checkRun.put("completed_at", LocalDateTime.now().toString());
            }
            
            Map<String, Object> output = new HashMap<>();
            output.put("title", "Smart Code Review Analysis");
            output.put("summary", summary);
            checkRun.put("output", output);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", "application/vnd.github.v3+json");
            if (!githubApiToken.isEmpty()) {
                headers.set("Authorization", "Bearer " + githubApiToken);
            }
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(checkRun, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Created check run for {}", repoFullName);
            }
            
        } catch (Exception e) {
            logger.error("Error creating check run: {}", e.getMessage());
        }
    }
    
    /**
     * Fetch file content from GitHub
     */
    public String fetchFileContent(String repoFullName, String path, String ref) {
        try {
            String url = String.format("%s/repos/%s/contents/%s?ref=%s", 
                githubApiBaseUrl, repoFullName, path, ref);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/vnd.github.v3+json");
            if (!githubApiToken.isEmpty()) {
                headers.set("Authorization", "Bearer " + githubApiToken);
            }
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode file = objectMapper.readTree(response.getBody());
                String content = file.path("content").asText();
                // GitHub returns base64 encoded content
                return new String(Base64.getDecoder().decode(content), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            logger.error("Error fetching file content from GitHub: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Fetch multiple files from a commit
     */
    public Map<String, String> fetchFilesFromCommit(String repoFullName, 
                                                    List<String> filePaths, 
                                                    String commitSha) {
        Map<String, String> fileContents = new HashMap<>();
        
        for (String path : filePaths) {
            if (isCodeFile(path)) {
                String content = fetchFileContent(repoFullName, path, commitSha);
                if (content != null) {
                    fileContents.put(path, content);
                }
            }
        }
        
        return fileContents;
    }

    private boolean isCodeFile(String fileName) {
        String[] extensions = {".java", ".py", ".js", ".ts", ".cpp", ".c", 
                              ".cs", ".go", ".rb", ".php", ".swift", ".kt"};
        String lower = fileName.toLowerCase();
        return Arrays.stream(extensions).anyMatch(lower::endsWith);
    }
    
    /**
     * Extract list of strings from JsonNode
     */
    private List<String> extractStringList(JsonNode node) {
        List<String> list = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode item : node) {
                list.add(item.asText());
            }
        }
        return list;
    }
    
    /**
     * Convert webhook payload to GitHubWebhookPayload DTO
     */
    public GitHubWebhookPayload parseWebhookPayload(String payload) throws IOException {
        return objectMapper.readValue(payload, GitHubWebhookPayload.class);
    }
}