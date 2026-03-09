package com.example.rssproducer.service;

import com.example.rssproducer.config.RssProperties;
import com.example.rssproducer.entity.RssFeed;
import com.example.rssproducer.entity.RssFeedEntry;
import com.example.rssproducer.repository.RssFeedRepository;
import com.example.rssproducer.repository.RssFeedEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RssPollingService {
    
    private final RssFeedRepository rssFeedRepository;
    private final RssFeedEntryRepository rssFeedEntryRepository;
    private final RssParserService rssParserService;
    private final KafkaProducerService kafkaProducerService;
    
    @Scheduled(fixedRateString = "${rss.polling.interval}")
    public void pollAllFeeds() {
        log.info("Starting RSS feed polling...");
        List<RssFeed> activeFeeds = rssFeedRepository.findByActiveTrue();
        
        for (RssFeed feed : activeFeeds) {
            try {
                List<RssFeedEntry> entries = rssParserService.parseFeed(feed);
                if (!entries.isEmpty()) {
                    List<RssFeedEntry> savedEntries = rssFeedEntryRepository.saveAll(entries);
                    kafkaProducerService.publishBatch(savedEntries);
                    log.info("Processed {} new entries from feed: {}", savedEntries.size(), feed.getName());
                }
            } catch (Exception e) {
                log.error("Error polling feed {}: {}", feed.getName(), e.getMessage());
            }
        }
        log.info("Completed RSS feed polling");
    }
    
    public void pollFeed(Long feedId) {
        rssFeedRepository.findById(feedId).ifPresent(feed -> {
            List<RssFeedEntry> entries = rssParserService.parseFeed(feed);
            if (!entries.isEmpty()) {
                List<RssFeedEntry> savedEntries = rssFeedEntryRepository.saveAll(entries);
                kafkaProducerService.publishBatch(savedEntries);
                log.info("Processed {} new entries from feed: {}", savedEntries.size(), feed.getName());
            }
        });
    }
}
