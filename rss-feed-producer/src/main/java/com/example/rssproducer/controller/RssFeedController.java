package com.example.rssproducer.controller;

import com.example.rssproducer.dto.AddFeedRequest;
import com.example.rssproducer.entity.RssFeed;
import com.example.rssproducer.repository.RssFeedRepository;
import com.example.rssproducer.service.RssPollingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/feeds")
@RequiredArgsConstructor
@Slf4j
public class RssFeedController {
    
    private final RssFeedRepository rssFeedRepository;
    private final RssPollingService rssPollingService;
    
    @GetMapping
    public ResponseEntity<List<RssFeed>> getAllFeeds() {
        return ResponseEntity.ok(rssFeedRepository.findAll());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<RssFeed> getFeedById(@PathVariable Long id) {
        return rssFeedRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/name/{name}")
    public ResponseEntity<RssFeed> getFeedByName(@PathVariable String name) {
        return rssFeedRepository.findByName(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<RssFeed> addFeed(@Valid @RequestBody AddFeedRequest request) {
        if (rssFeedRepository.findByUrl(request.getUrl()).isPresent()) {
            return ResponseEntity.badRequest().build();
        }
        
        RssFeed feed = RssFeed.builder()
                .name(request.getName())
                .url(request.getUrl())
                .description(request.getDescription())
                .active(true)
                .build();
        
        RssFeed savedFeed = rssFeedRepository.save(feed);
        log.info("Added new RSS feed: {}", savedFeed.getName());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(savedFeed);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<RssFeed> updateFeed(@PathVariable Long id, @RequestBody RssFeed feedDetails) {
        return rssFeedRepository.findById(id)
                .map(feed -> {
                    feed.setName(feedDetails.getName());
                    feed.setUrl(feedDetails.getUrl());
                    feed.setDescription(feedDetails.getDescription());
                    feed.setActive(feedDetails.getActive());
                    return ResponseEntity.ok(rssFeedRepository.save(feed));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFeed(@PathVariable Long id) {
        if (rssFeedRepository.existsById(id)) {
            rssFeedRepository.deleteById(id);
            log.info("Deleted RSS feed with id: {}", id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
    
    @PostMapping("/{id}/poll")
    public ResponseEntity<String> pollFeed(@PathVariable Long id) {
        rssPollingService.pollFeed(id);
        return ResponseEntity.ok("Polling initiated for feed: " + id);
    }
    
    @PostMapping("/{id}/toggle")
    public ResponseEntity<RssFeed> toggleFeed(@PathVariable Long id) {
        return rssFeedRepository.findById(id)
                .map(feed -> {
                    feed.setActive(!feed.getActive());
                    return ResponseEntity.ok(rssFeedRepository.save(feed));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
