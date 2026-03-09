package com.example.rssconsumer.service;

import com.example.rssconsumer.config.OllamaConfig;
import com.example.rssconsumer.entity.NewsArticle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import jakarta.annotation.PostConstruct;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OllamaService {
    
    private final OllamaConfig ollamaConfig;
    private final WebClient webClient;
    
    private final Map<String, CachedResponse> responseCache = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() {
        log.info("Ollama Service initialized with config: baseUrl={}, model={}, batchSize={}, cacheEnabled={}",
                ollamaConfig.getBaseUrl(), ollamaConfig.getModel(), 
                ollamaConfig.getBatchSize(), ollamaConfig.isCacheEnabled());
    }
    
    public List<Long> findRelevantArticles(String prompt, List<NewsArticle> articles) {
        if (!ollamaConfig.isEnabled()) {
            log.debug("Ollama is disabled, returning all articles");
            return articles.stream().map(NewsArticle::getId).collect(Collectors.toList());
        }
        
        if (articles.isEmpty()) {
            return Collections.emptyList();
        }
        
        try {
            String articlesText = buildArticlesText(articles);
            String searchPrompt = buildSearchPrompt(prompt, articlesText);
            
            String response = queryOllamaWithCache(searchPrompt);
            return parseOllamaResponse(response, articles);
        } catch (Exception e) {
            log.error("Failed to search with Ollama: {}", e.getMessage());
            return articles.stream().map(NewsArticle::getId).collect(Collectors.toList());
        }
    }
    
    private Mono<String> queryOllamaWithCacheAsync(String prompt) {
        String cacheKey = generateCacheKey(prompt, "");
        
        if (ollamaConfig.isCacheEnabled()) {
            CachedResponse cached = responseCache.get(cacheKey);
            if (cached != null && !cached.isExpired()) {
                log.debug("Cache hit for prompt");
                return Mono.just(cached.getResponse());
            }
        }
        
        return queryOllamaAsync(prompt)
                .doOnNext(response -> {
                    if (ollamaConfig.isCacheEnabled()) {
                        responseCache.put(cacheKey, new CachedResponse(
                                response, 
                                Duration.ofMinutes(ollamaConfig.getCacheTtlMinutes())
                        ));
                    }
                })
                .onErrorResume(e -> {
                    log.error("Ollama query failed: {}", e.getMessage());
                    return Mono.empty();
                });
    }
    
    private String queryOllamaWithCache(String prompt) {
        String cacheKey = generateCacheKey(prompt, "");
        
        if (ollamaConfig.isCacheEnabled()) {
            CachedResponse cached = responseCache.get(cacheKey);
            if (cached != null && !cached.isExpired()) {
                log.debug("Cache hit for prompt");
                return cached.getResponse();
            }
        }
        
        try {
            String response = queryOllamaAsync(prompt).block(Duration.ofSeconds(60));
            if (response != null && ollamaConfig.isCacheEnabled()) {
                responseCache.put(cacheKey, new CachedResponse(
                        response, 
                        Duration.ofMinutes(ollamaConfig.getCacheTtlMinutes())
                ));
            }
            return response;
        } catch (Exception e) {
            log.error("Failed to query Ollama: {}", e.getMessage());
            return null;
        }
    }
    
    private Mono<String> queryOllamaAsync(String prompt) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", ollamaConfig.getModel());
        requestBody.put("prompt", prompt);
        requestBody.put("stream", false);
        
        return webClient.post()
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (String) response.getOrDefault("response", ""))
                .timeout(Duration.ofMillis(ollamaConfig.getResponseTimeout()));
    }
    
    private String buildArticlesText(List<NewsArticle> articles) {
        StringBuilder sb = new StringBuilder();
        for (NewsArticle article : articles) {
            sb.append("ID: ").append(article.getId())
              .append(", Title: ").append(article.getTitle())
              .append(", Description: ").append(article.getDescription())
              .append("; ");
        }
        return sb.toString();
    }
    
    private String buildSearchPrompt(String prompt, String articlesText) {
        return ollamaConfig.getSearchPromptTemplate()
                .replace("{prompt}", prompt)
                .replace("{articles}", articlesText);
    }
    
    private List<Long> parseOllamaResponse(String response, List<NewsArticle> articles) {
        if (response == null || response.toLowerCase().contains("none")) {
            return Collections.emptyList();
        }
        
        List<Long> relevantIds = new ArrayList<>();
        String[] parts = response.split(",");
        
        for (String part : parts) {
            try {
                String idStr = part.replaceAll("[^0-9]", "").trim();
                if (!idStr.isEmpty()) {
                    relevantIds.add(Long.parseLong(idStr));
                }
            } catch (NumberFormatException e) {
                log.warn("Could not parse article ID: {}", part);
            }
        }
        
        return relevantIds;
    }
    
    private String generateCacheKey(String prompt, String extra) {
        String content = prompt + "|" + extra;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(content.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(content.hashCode());
        }
    }
    
    public boolean isOllamaAvailable() {
        try {
            webClient.get()
                    .uri("/api/tags")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            return true;
        } catch (Exception e) {
            log.warn("Ollama is not available: {}", e.getMessage());
            return false;
        }
    }
    
    public int getCacheSize() {
        return responseCache.size();
    }
    
    public void clearCache() {
        responseCache.clear();
        log.info("Ollama cache cleared");
    }
    
    private static class CachedResponse {
        private final String response;
        private final long createdAt;
        private final long ttlMillis;
        
        public CachedResponse(String response, Duration ttl) {
            this.response = response;
            this.createdAt = System.currentTimeMillis();
            this.ttlMillis = ttl.toMillis();
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - createdAt > ttlMillis;
        }
        
        public String getResponse() {
            return response;
        }
    }
}
