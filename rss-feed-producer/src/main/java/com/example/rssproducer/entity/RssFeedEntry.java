package com.example.rssproducer.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "rss_feed_entries", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"feed_id", "entry_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RssFeedEntry {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feed_id", nullable = false)
    private RssFeed feed;
    
    @Column(nullable = false)
    private String entryId;
    
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column
    private String link;
    
    @Column
    private LocalDateTime publishedDate;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @Column(columnDefinition = "TEXT")
    private String enrichedContent;
    
    @Column
    private Boolean processed;
    
    @Column
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (processed == null) {
            processed = false;
        }
    }
}
