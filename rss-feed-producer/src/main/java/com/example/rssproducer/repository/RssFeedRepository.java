package com.example.rssproducer.repository;

import com.example.rssproducer.entity.RssFeed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RssFeedRepository extends JpaRepository<RssFeed, Long> {
    
    Optional<RssFeed> findByName(String name);
    
    Optional<RssFeed> findByUrl(String url);
    
    List<RssFeed> findByActiveTrue();
}
