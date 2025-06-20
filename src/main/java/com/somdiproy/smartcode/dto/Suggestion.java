package com.somdiproy.smartcode.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Suggestion {
    private String title;
    private String description;
    private String category;
    private String impact;
    private String implementation;
}