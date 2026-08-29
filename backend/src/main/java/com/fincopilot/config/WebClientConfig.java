package com.fincopilot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

//scanned by spring at startup 
@Configuration
public class WebClientConfig {

    // injects baseURL
    @Value("${ai-service.base-url}")
    private String aiServiceBaseUrl;

    // spring initializeaza clasa, stocheaza obiectul in IoC si il injecteaza in
    // ChatService pt requests
    @Bean
    public WebClient aiServiceWebClient(WebClient.Builder builder) {
        return builder.baseUrl(aiServiceBaseUrl).build();
    }
}
