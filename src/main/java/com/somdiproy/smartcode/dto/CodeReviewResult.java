package com.somdiproy.smartcode.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class CodeReviewResult {
    private String summary;
    private Double overallScore;
    private List<CodeIssue> issues;
    private List<Suggestion> suggestions;
    private SecurityAnalysis security;
    private PerformanceAnalysis performance;
    private QualityMetrics quality;
    private Map<String, Object> metadata;
}