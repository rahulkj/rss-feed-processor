package com.example.rssproducer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddFeedRequest {
    
    @NotBlank(message = "Feed name is required")
    private String name;
    
    @NotBlank(message = "Feed URL is required")
    private String url;
    
    private String description;
}
