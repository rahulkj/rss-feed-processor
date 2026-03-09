package com.example.rssproducer.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RssEventDto {
    
    private String feedKey;
    private String feedUrl;
    private String feedName;
    private String title;
    private String description;
    private String link;
    private LocalDateTime publishedDate;
    private String content;
    private String enrichedContent;
}
