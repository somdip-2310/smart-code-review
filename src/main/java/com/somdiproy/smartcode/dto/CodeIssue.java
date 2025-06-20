package com.somdiproy.smartcode.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CodeIssue {
    private IssueSeverity severity;
    private IssueType type;
    private String title;
    private String description;
    private Integer lineNumber;
    private String codeSnippet;
    private String suggestion;
    private String fileName;
}