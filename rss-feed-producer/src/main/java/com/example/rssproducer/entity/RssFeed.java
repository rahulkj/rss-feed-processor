package com.example.rssproducer.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "rss_feeds")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RssFeed {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String name;
    
    @Column(nullable = false)
    private String url;
    
    @Column
    private String description;
    
    @Column
    private Boolean active;
    
    @Column
    private LocalDateTime lastPolledAt;
    
    @Column
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (active == null) {
            active = true;
        }
    }
}
