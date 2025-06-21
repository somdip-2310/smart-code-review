package com.somdiproy.smartcode.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeAnalysisRequest {
    
    @NotBlank(message = "Session token is required")
    private String sessionToken;
    
    @NotBlank(message = "Code is required")
    @Size(max = 100000, message = "Code must be less than 100KB")
    private String code;
    
    @NotBlank(message = "Language is required")
    private String language;
    
    private String fileName;
}