package com.example.rssconsumer.controller;

import com.example.rssconsumer.dto.SearchRequest;
import com.example.rssconsumer.entity.NewsArticle;
import com.example.rssconsumer.service.NewsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
@Slf4j
public class NewsController {
    
    private final NewsService newsService;
    
    @GetMapping
    public ResponseEntity<List<NewsArticle>> getAllNews() {
        return ResponseEntity.ok(newsService.getAllNews());
    }
    
    @GetMapping("/feed/{feedKey}")
    public ResponseEntity<List<NewsArticle>> getNewsByFeedKey(@PathVariable String feedKey) {
        return ResponseEntity.ok(newsService.getNewsByFeedKey(feedKey));
    }
    
    @PostMapping("/search")
    public ResponseEntity<List<NewsArticle>> searchNews(@Valid @RequestBody SearchRequest request) {
        return ResponseEntity.ok(newsService.searchNews(request));
    }
}
