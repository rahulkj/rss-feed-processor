package com.example.rssproducer.service;

import com.example.rssproducer.entity.RssFeed;
import com.example.rssproducer.entity.RssFeedEntry;
import com.example.rssproducer.repository.RssFeedEntryRepository;
import com.example.rssproducer.repository.RssFeedRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RssParserService {
    
    private final RssFeedRepository rssFeedRepository;
    private final RssFeedEntryRepository rssFeedEntryRepository;
    
    public List<RssFeedEntry> parseFeed(RssFeed feed) {
        List<RssFeedEntry> entries = new ArrayList<>();
        
        try {
            URL url = new URL(feed.getUrl());
            SyndFeedInput input = new SyndFeedInput();
            SyndFeed syndFeed = input.build(new XmlReader(url));
            
            List<SyndEntry> syndEntries = syndFeed.getEntries();
            log.debug("Parsed {} entries from feed: {}", syndEntries.size(), feed.getName());
            
            for (SyndEntry syndEntry : syndEntries) {
                String entryId = extractEntryId(syndEntry);
                
                if (rssFeedEntryRepository.existsByFeedIdAndEntryId(feed.getId(), entryId)) {
                    log.debug("Entry already exists: {}", entryId);
                    continue;
                }
                
                RssFeedEntry entry = RssFeedEntry.builder()
                        .feed(feed)
                        .entryId(entryId)
                        .title(syndEntry.getTitle())
                        .description(syndEntry.getDescription() != null ? 
                                syndEntry.getDescription().getValue() : null)
                        .link(syndEntry.getLink())
                        .publishedDate(convertToLocalDateTime(syndEntry.getPublishedDate()))
                        .content(syndEntry.getContents() != null && !syndEntry.getContents().isEmpty() ?
                                syndEntry.getContents().get(0).getValue() : null)
                        .processed(false)
                        .build();
                
                entries.add(entry);
            }
            
            feed.setLastPolledAt(LocalDateTime.now());
            rssFeedRepository.save(feed);
            
        } catch (Exception e) {
            log.error("Failed to parse feed {}: {}", feed.getUrl(), e.getMessage());
        }
        
        return entries;
    }
    
    private String extractEntryId(SyndEntry entry) {
        if (entry.getUri() != null && !entry.getUri().isEmpty()) {
            return entry.getUri();
        }
        if (entry.getLink() != null && !entry.getLink().isEmpty()) {
            return entry.getLink();
        }
        return String.valueOf(entry.getTitle().hashCode());
    }
    
    private LocalDateTime convertToLocalDateTime(java.util.Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}
