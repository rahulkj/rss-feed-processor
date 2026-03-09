package com.example.rssproducer.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {
    
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    
    public static final String RSS_EVENTS_TOPIC = "rss-events";
    
    @Bean
    public NewTopic rssEventsTopic() {
        return TopicBuilder.name(RSS_EVENTS_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
