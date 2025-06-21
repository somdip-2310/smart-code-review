package com.somdiproy.smartcode.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResponse {
    private boolean success;
    private String analysisId;
    private AnalysisStatus status;
    private String message;
    private CodeReviewResult result;
    private long createdAt;
    private long updatedAt;
    private int progressPercentage;
}