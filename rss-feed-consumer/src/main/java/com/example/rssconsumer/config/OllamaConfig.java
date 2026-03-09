package com.example.rssconsumer.config;

import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ollama")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OllamaConfig {
    
    @Builder.Default
    private String baseUrl = "http://localhost:11434";
    
    @Builder.Default
    private String model = "deepseek";
    
    @Builder.Default
    private boolean enabled = true;
    
    @Builder.Default
    private String searchPromptTemplate = "Find relevant articles: {prompt}. Articles: {articles}";
    
    @Builder.Default
    private int connectionPoolSize = 50;
    
    @Builder.Default
    private long connectTimeout = 10000;
    
    @Builder.Default
    private long responseTimeout = 60000;
    
    @Builder.Default
    private int batchSize = 10;
    
    @Builder.Default
    private boolean cacheEnabled = true;
    
    @Builder.Default
    private long cacheTtlMinutes = 60;
}
