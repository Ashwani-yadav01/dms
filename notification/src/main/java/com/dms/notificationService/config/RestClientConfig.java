package com.dms.notificationService.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Value("${dms.services.user-service.url:http://localhost:8081}")
    private String userServiceBaseUrl;

    @Bean
    public RestClient userRestClient() {
        return RestClient.builder()
                .baseUrl(userServiceBaseUrl)
                .build();
    }
}