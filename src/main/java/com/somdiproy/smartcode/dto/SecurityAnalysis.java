package com.somdiproy.smartcode.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class SecurityAnalysis {
    private Double securityScore;
    private List<String> vulnerabilities;
    private List<String> recommendations;
    private Boolean hasSecurityIssues;
}