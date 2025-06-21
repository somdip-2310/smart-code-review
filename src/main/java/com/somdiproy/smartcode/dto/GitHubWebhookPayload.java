package com.somdiproy.smartcode.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * GitHub Webhook Payload DTOs
 * 
 * Data transfer objects for various GitHub webhook events
 * 
 * @author Somdip Roy
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubWebhookPayload {
    
    private String action;
    private Repository repository;
    private Sender sender;
    private Organization organization;
    
    // For push events
    private String ref;
    private String before;
    private String after;
    private List<Commit> commits;
    private Pusher pusher;
    
    // For pull request events
    @JsonProperty("pull_request")
    private PullRequest pullRequest;
    
    // For issue events
    private Issue issue;
    
    /**
     * Repository information
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Repository {
        private Long id;
        private String name;
        @JsonProperty("full_name")
        private String fullName;
        private Boolean privateRepo;
        private Owner owner;
        @JsonProperty("html_url")
        private String htmlUrl;
        private String description;
        @JsonProperty("clone_url")
        private String cloneUrl;
        @JsonProperty("ssh_url")
        private String sshUrl;
        @JsonProperty("default_branch")
        private String defaultBranch;
        private String language;
        @JsonProperty("created_at")
        private String createdAt;
        @JsonProperty("updated_at")
        private String updatedAt;
    }
    
    /**
     * Owner/User information
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Owner {
        private String login;
        private Long id;
        private String type;
        @JsonProperty("avatar_url")
        private String avatarUrl;
        @JsonProperty("html_url")
        private String htmlUrl;
    }
    
    /**
     * Sender information
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Sender {
        private String login;
        private Long id;
        @JsonProperty("avatar_url")
        private String avatarUrl;
        private String type;
    }
    
    /**
     * Organization information
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Organization {
        private String login;
        private Long id;
        private String url;
        @JsonProperty("avatar_url")
        private String avatarUrl;
    }
    
    /**
     * Commit information
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Commit {
        private String id;
        @JsonProperty("tree_id")
        private String treeId;
        private Boolean distinct;
        private String message;
        private String timestamp;
        private String url;
        private Author author;
        private Author committer;
        private List<String> added;
        private List<String> removed;
        private List<String> modified;
    }
    
    /**
     * Author/Committer information
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Author {
        private String name;
        private String email;
        private String username;
    }
    
    /**
     * Pusher information
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Pusher {
        private String name;
        private String email;
    }
    
    /**
     * Pull Request information
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PullRequest {
        private Long id;
        private Integer number;
        private String state;
        private Boolean locked;
        private String title;
        private String body;
        private Owner user;
        @JsonProperty("created_at")
        private String createdAt;
        @JsonProperty("updated_at")
        private String updatedAt;
        @JsonProperty("closed_at")
        private String closedAt;
        @JsonProperty("merged_at")
        private String mergedAt;
        @JsonProperty("merge_commit_sha")
        private String mergeCommitSha;
        private BranchInfo head;
        private BranchInfo base;
        @JsonProperty("html_url")
        private String htmlUrl;
        @JsonProperty("diff_url")
        private String diffUrl;
        @JsonProperty("patch_url")
        private String patchUrl;
        private Boolean merged;
        private Boolean mergeable;
        @JsonProperty("changed_files")
        private Integer changedFiles;
        private Integer additions;
        private Integer deletions;
        private Integer commits;
    }
    
    /**
     * Branch information for PR
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BranchInfo {
        private String label;
        private String ref;
        private String sha;
        private Owner user;
        private Repository repo;
    }
    
    /**
     * Issue information
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Issue {
        private Long id;
        private Integer number;
        private String title;
        private String body;
        private String state;
        private Owner user;
        private List<Label> labels;
        private Owner assignee;
        private List<Owner> assignees;
        @JsonProperty("created_at")
        private String createdAt;
        @JsonProperty("updated_at")
        private String updatedAt;
        @JsonProperty("closed_at")
        private String closedAt;
        @JsonProperty("html_url")
        private String htmlUrl;
    }
    
    /**
     * Label information
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Label {
        private Long id;
        private String name;
        private String color;
        private Boolean defaultLabel;
        private String description;
    }
}