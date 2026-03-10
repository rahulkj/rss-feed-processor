package com.example.rssproducer.service;

import com.example.rssproducer.config.KafkaConfig;
import com.example.rssproducer.dto.RssEventDto;
import com.example.rssproducer.entity.RssFeed;
import com.example.rssproducer.entity.RssFeedEntry;
import com.example.rssproducer.repository.RssFeedEntryRepository;
import com.example.rssproducer.repository.RssFeedRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {
    
    private final KafkaTemplate<String, RssEventDto> kafkaTemplate;
    private final RssFeedEntryRepository rssFeedEntryRepository;
    private final OllamaService ollamaService;
    
    public void publishRssEvent(RssFeedEntry entry) {
        RssFeed feed = entry.getFeed();
        
        RssEventDto event = RssEventDto.builder()
                .feedKey(feed.getName())
                .feedUrl(feed.getUrl())
                .feedName(feed.getName())
                .title(entry.getTitle())
                .description(entry.getDescription())
                .link(entry.getLink())
                .publishedDate(entry.getPublishedDate())
                .content(entry.getContent())
                .enrichedContent(entry.getEnrichedContent())
                .build();
        
        CompletableFuture<SendResult<String, RssEventDto>> future = 
                kafkaTemplate.send(KafkaConfig.RSS_EVENTS_TOPIC, feed.getName(), event);
        
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to send RSS event: {}", ex.getMessage());
            } else {
                log.debug("Sent RSS event to Kafka: {} - {}", feed.getName(), entry.getTitle());
                entry.setProcessed(true);
                rssFeedEntryRepository.save(entry);
            }
        });
    }
    
    public void publishBatch(List<RssFeedEntry> entries) {
        if (entries.isEmpty()) {
            return;
        }
        
        entries.forEach(entry -> publishSingleWithEnrichment(entry));
        
        log.info("Published {} RSS events to Kafka", entries.size());
    }
    
    private void publishSingleWithEnrichment(RssFeedEntry entry) {
        RssFeed feed = entry.getFeed();
        
        String enrichedContent = ollamaService.enrichSingle(entry);
        
        RssEventDto event = RssEventDto.builder()
                .feedKey(feed.getName())
                .feedUrl(feed.getUrl())
                .feedName(feed.getName())
                .title(entry.getTitle())
                .description(entry.getDescription())
                .link(entry.getLink())
                .publishedDate(entry.getPublishedDate())
                .content(entry.getContent())
                .enrichedContent(enrichedContent)
                .build();
        
        CompletableFuture<SendResult<String, RssEventDto>> future = 
                kafkaTemplate.send(KafkaConfig.RSS_EVENTS_TOPIC, feed.getName(), event);
        
        final String finalEnriched = enrichedContent;
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to send RSS event: {}", ex.getMessage());
            } else {
                log.debug("Sent RSS event to Kafka: {} - {}", feed.getName(), entry.getTitle());
                entry.setProcessed(true);
                entry.setEnrichedContent(finalEnriched);
                rssFeedEntryRepository.save(entry);
            }
        });
    }
}
