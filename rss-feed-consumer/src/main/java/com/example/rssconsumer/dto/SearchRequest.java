package com.example.rssconsumer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchRequest {
    
    @NotBlank(message = "Search prompt is required")
    private String prompt;
    
    private String feedKey;
}
