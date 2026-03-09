package com.example.rssproducer.config;

import com.example.rssproducer.entity.RssFeed;
import com.example.rssproducer.repository.RssFeedRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {
    
    private final RssFeedRepository rssFeedRepository;
    private final RssProperties rssProperties;
    
    @Override
    public void run(String... args) {
        if (rssFeedRepository.count() == 0 && rssProperties.getFeeds() != null) {
            log.info("Initializing RSS feeds from configuration...");
            List<RssFeed> feeds = rssProperties.getFeeds().stream()
                    .map(feedConfig -> RssFeed.builder()
                            .name(feedConfig.getName())
                            .url(feedConfig.getUrl())
                            .active(true)
                            .build())
                    .toList();
            
            rssFeedRepository.saveAll(feeds);
            log.info("Initialized {} RSS feeds", feeds.size());
        }
    }
}
