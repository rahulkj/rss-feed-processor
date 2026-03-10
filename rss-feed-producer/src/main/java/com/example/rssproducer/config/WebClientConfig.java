package com.example.rssproducer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import java.time.Duration;

@Configuration
public class WebClientConfig {
    
    @Bean
    public ConnectionProvider connectionProvider(OllamaConfig ollamaConfig) {
        return ConnectionProvider.builder("ollama-pool")
                // .maxConnections(ollamaConfig.getConnectionPoolSize())
                .pendingAcquireTimeout(Duration.ofMillis(30000))
                .pendingAcquireMaxCount(-1)
                .maxIdleTime(Duration.ofSeconds(30))
                .build();
    }
    
    @Bean
    public WebClient webClient(ConnectionProvider connectionProvider, OllamaConfig ollamaConfig) {
        HttpClient httpClient = HttpClient.create(connectionProvider)
                .responseTimeout(Duration.ofMillis(ollamaConfig.getResponseTimeout()))
                .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) ollamaConfig.getConnectTimeout());
        
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(16 * 1024 * 1024))
                .build();
        
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(strategies)
                .baseUrl(ollamaConfig.getBaseUrl())
                .build();
    }
}
