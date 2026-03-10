package com.example.rssproducer.service;

import com.example.rssproducer.config.OllamaConfig;
import com.example.rssproducer.dto.RssEventDto;
import com.example.rssproducer.entity.RssFeedEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
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
        
        List<String> results = new ArrayList<>();
        for (RssEventDto entry : entriesToEnrich) {
            String enriched = enrichSingleDto(entry);
            results.add(enriched);
        }
        
        List<String> allResults = entries.stream().map(e -> (String) null).collect(Collectors.toList());
        AtomicInteger idx = new AtomicInteger(0);
        entries.forEach(entry -> {
            if (shouldEnrich(entry)) {
                allResults.set(entries.indexOf(entry), results.get(idx.getAndIncrement()));
            }
        });
        
        return allResults;
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
    
    public String enrichSingle(RssFeedEntry entry) {
        RssEventDto dto = RssEventDto.builder()
                .title(entry.getTitle())
                .description(entry.getDescription())
                .build();
        
        return enrichSingleDto(dto);
    }
    
    private String enrichSingleDto(RssEventDto entry) {
        String cacheKey = generateCacheKey(entry.getTitle(), entry.getDescription());
        
        if (ollamaConfig.isCacheEnabled()) {
            CachedResponse cached = responseCache.get(cacheKey);
            if (cached != null && !cached.isExpired()) {
                log.debug("Cache hit for: {}", entry.getTitle());
                return cached.getResponse();
            }
        }
        
        String response = enrichSingleAsync(entry)
                .block(Duration.ofMillis(ollamaConfig.getResponseTimeout()));
        
        if (response != null && ollamaConfig.isCacheEnabled()) {
            responseCache.put(cacheKey, new CachedResponse(
                    response, 
                    Duration.ofMinutes(ollamaConfig.getCacheTtlMinutes())
            ));
        }
        
        return response;
    }
    
    private Mono<String> enrichSingleAsync(RssEventDto entry) {
        String prompt = buildPrompt(entry.getTitle(), entry.getDescription());
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", ollamaConfig.getModel());
        requestBody.put("prompt", prompt);
        requestBody.put("stream", false);
        
        return webClient.post()
                .uri("/api/generate")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (String) response.getOrDefault("response", ""))
                .timeout(Duration.ofMillis(ollamaConfig.getResponseTimeout()));
    }
    
    private String buildPrompt(String title, String description) {
        String template = ollamaConfig.getPromptTemplate();
        return template
                .replace("{title}", title != null ? title : "")
                .replace("{description}", description != null ? description : "");
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
