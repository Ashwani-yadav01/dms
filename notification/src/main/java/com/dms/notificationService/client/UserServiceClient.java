package com.dms.notificationService.client;

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

    public List<String> fetchNearbyUserEmails(Double latitude, Double longitude, Double radiusKm) {
        try {
            return userRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/users/nearby/emails")
                            .queryParam("latitude", latitude)
                            .queryParam("longitude", longitude)
                            .queryParam("radiusKm", radiusKm != null ? radiusKm : 5.0)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<String>>() {});
        } catch (Exception e) {
            log.error("Failed to fetch nearby user emails from User-Service: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}