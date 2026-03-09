package com.example.rssconsumer.repository;

import com.example.rssconsumer.entity.NewsArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NewsArticleRepository extends JpaRepository<NewsArticle, Long> {
    
    List<NewsArticle> findByFeedKeyOrderByPublishedDateDesc(String feedKey);
    
    List<NewsArticle> findTop100ByOrderByPublishedDateDesc();
    
    List<NewsArticle> findByFeedKeyInOrderByPublishedDateDesc(List<String> feedKeys);
}
