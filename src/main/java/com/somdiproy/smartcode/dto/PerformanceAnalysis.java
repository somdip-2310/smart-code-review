package com.somdiproy.smartcode.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class PerformanceAnalysis {
    private Double performanceScore;
    private List<String> bottlenecks;
    private List<String> optimizations;
    private String complexity;
}