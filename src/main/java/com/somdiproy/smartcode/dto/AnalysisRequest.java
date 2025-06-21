package com.somdiproy.smartcode.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisRequest {
    private AnalysisType type;
    private String sessionToken;
    private String fileName;
    private long fileSize;
    private String language;
    private String clientIp;
}