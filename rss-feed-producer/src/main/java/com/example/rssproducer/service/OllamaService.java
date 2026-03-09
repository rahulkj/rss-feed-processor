package com.example.rssproducer.service;

import com.example.rssproducer.config.OllamaConfig;
import com.example.rssproducer.dto.RssEventDto;
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
import java.util.concurrent.atomic.AtomicInteger;
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
    
    public List<String> enrichContentBatch(List<RssEventDto> entries) {
        if (!ollamaConfig.isEnabled()) {
            log.debug("Ollama enrichment is disabled");
            return Collections.nCopies(entries.size(), null);
        }
        
        if (entries.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<RssEventDto> entriesToEnrich = entries.stream()
                .filter(this::shouldEnrich)
                .collect(Collectors.toList());
        
        if (entriesToEnrich.isEmpty()) {
            log.debug("No entries need enrichment");
            return entries.stream().map(e -> (String) null).collect(Collectors.toList());
        }
        
        return Flux.fromIterable(entriesToEnrich)
                .flatMap(this::enrichWithCache, ollamaConfig.getConnectionPoolSize())
                .subscribeOn(Schedulers.boundedElastic())
                .collectList()
                .block(Duration.ofSeconds(60));
    }
    
    private Mono<String> enrichWithCache(RssEventDto entry) {
        String cacheKey = generateCacheKey(entry.getTitle(), entry.getDescription());
        
        if (ollamaConfig.isCacheEnabled()) {
            CachedResponse cached = responseCache.get(cacheKey);
            if (cached != null && !cached.isExpired()) {
                log.debug("Cache hit for: {}", entry.getTitle());
                return Mono.just(cached.getResponse());
            }
        }
        
        return enrichSingle(entry)
                .doOnNext(response -> {
                    if (ollamaConfig.isCacheEnabled()) {
                        responseCache.put(cacheKey, new CachedResponse(
                                response, 
                                Duration.ofMinutes(ollamaConfig.getCacheTtlMinutes())
                        ));
                    }
                })
                .onErrorResume(e -> {
                    log.error("Failed to enrich content: {}", e.getMessage());
                    return Mono.empty();
                });
    }
    
    private Mono<String> enrichSingle(RssEventDto entry) {
        String prompt = buildPrompt(entry.getTitle(), entry.getDescription());
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", ollamaConfig.getModel());
        requestBody.put("prompt", prompt);
        requestBody.put("stream", false);
        
        return webClient.post()
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (String) response.getOrDefault("response", ""))
                .timeout(Duration.ofMillis(ollamaConfig.getResponseTimeout()))
                .doOnNext(response -> log.debug("Enriched: {}", entry.getTitle()));
    }
    
    private String buildPrompt(String title, String description) {
        String template = ollamaConfig.getPromptTemplate();
        return template
                .replace("{title}", title != null ? title : "")
                .replace("{description}", description != null ? description : "");
    }
    
    private boolean shouldEnrich(RssEventDto entry) {
        String title = entry.getTitle();
        if (title == null || title.isBlank()) {
            return false;
        }
        
        String lowerTitle = title.toLowerCase();
        
        List<String> priorityKeywords = Arrays.asList(
                "breaking", "exclusive", "urgent", "alert", "news",
                "update", "announcement", "release"
        );
        
        for (String keyword : priorityKeywords) {
            if (lowerTitle.contains(keyword)) {
                return true;
            }
        }
        
        return title.length() > 30;
    }
    
    private String generateCacheKey(String title, String description) {
        String content = (title != null ? title : "") + "|" + (description != null ? description : "");
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
