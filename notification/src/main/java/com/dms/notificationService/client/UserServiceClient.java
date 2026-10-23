package com.dms.notificationService.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceClient {

    private final RestClient userRestClient;

    @CircuitBreaker(name = "userServiceClient", fallbackMethod = "fetchNearbyUserEmailsFallback")
    public List<String> fetchNearbyUserEmails(Double latitude, Double longitude, Double radiusKm) {
        List<String> emails = userRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/users/nearby/emails")
                        .queryParam("latitude", latitude)
                        .queryParam("longitude", longitude)
                        .queryParam("radiusKm", radiusKm != null ? radiusKm : 5.0)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<String>>() {});

        return emails != null ? emails : Collections.emptyList();
    }

    public List<String> fetchNearbyUserEmailsFallback(Double latitude, Double longitude, Double radiusKm, Throwable ex) {
        log.warn("Circuit breaker [userServiceClient] fallback triggered: {}", ex.getMessage());
        return Collections.emptyList();
    }
}
