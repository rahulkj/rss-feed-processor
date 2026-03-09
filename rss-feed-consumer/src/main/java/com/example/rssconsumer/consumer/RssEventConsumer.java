package com.example.rssconsumer.consumer;

import com.example.rssconsumer.dto.RssEventDto;
import com.example.rssconsumer.entity.NewsArticle;
import com.example.rssconsumer.repository.NewsArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RssEventConsumer {
    
    private final NewsArticleRepository newsArticleRepository;
    
    @KafkaListener(topics = "rss-events", groupId = "rss-consumer-group")
    public void consume(RssEventDto event) {
        log.info("Received RSS event from feed: {}", event.getFeedKey());
        
        NewsArticle article = NewsArticle.builder()
                .feedKey(event.getFeedKey())
                .feedUrl(event.getFeedUrl())
                .feedName(event.getFeedName())
                .title(event.getTitle())
                .description(event.getDescription())
                .link(event.getLink())
                .publishedDate(event.getPublishedDate())
                .content(event.getContent())
                .enrichedContent(event.getEnrichedContent())
                .build();
        
        newsArticleRepository.save(article);
        log.info("Saved article: {}", article.getTitle());
    }
}
