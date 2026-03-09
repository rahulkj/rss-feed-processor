package com.example.rssproducer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RssFeedProducerApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(RssFeedProducerApplication.class, args);
    }
}
