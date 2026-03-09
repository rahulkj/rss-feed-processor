package com.example.rssproducer.config;

import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "rss")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RssProperties {
    
    private long pollingInterval;
    private List<FeedConfig> feeds;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeedConfig {
        private String name;
        private String url;
    }
}
