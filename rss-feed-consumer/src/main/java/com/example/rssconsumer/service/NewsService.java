package com.example.rssconsumer.service;

import com.example.rssconsumer.dto.SearchRequest;
import com.example.rssconsumer.entity.NewsArticle;
import com.example.rssconsumer.repository.NewsArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsService {
    
    private final NewsArticleRepository newsArticleRepository;
    private final OllamaService ollamaService;
    
    public List<NewsArticle> getAllNews() {
        return newsArticleRepository.findTop100ByOrderByPublishedDateDesc();
    }
    
    public List<NewsArticle> getNewsByFeedKey(String feedKey) {
        return newsArticleRepository.findByFeedKeyOrderByPublishedDateDesc(feedKey);
    }
    
    public List<NewsArticle> searchNews(SearchRequest request) {
        List<NewsArticle> articles;
        
        if (request.getFeedKey() != null && !request.getFeedKey().isEmpty()) {
            articles = newsArticleRepository.findByFeedKeyOrderByPublishedDateDesc(request.getFeedKey());
        } else {
            articles = newsArticleRepository.findTop100ByOrderByPublishedDateDesc();
        }
        
        if (articles.isEmpty()) {
            return List.of();
        }
        
        List<Long> relevantIds = ollamaService.findRelevantArticles(request.getPrompt(), articles);
        
        if (relevantIds.isEmpty()) {
            return List.of();
        }
        
        return articles.stream()
                .filter(article -> relevantIds.contains(article.getId()))
                .toList();
    }
}
