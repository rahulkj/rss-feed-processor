package com.example.rssproducer.repository;

import com.example.rssproducer.entity.RssFeedEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RssFeedEntryRepository extends JpaRepository<RssFeedEntry, Long> {
    
    Optional<RssFeedEntry> findByFeedIdAndEntryId(Long feedId, String entryId);
    
    boolean existsByFeedIdAndEntryId(Long feedId, String entryId);
}
