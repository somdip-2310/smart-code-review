package com.somdiproy.smartcode.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QualityMetrics {
    private Double maintainabilityScore;
    private Double readabilityScore;
    private Integer linesOfCode;
    private Integer complexityScore;
    private Double testCoverage;
    private Integer duplicateLines;
}