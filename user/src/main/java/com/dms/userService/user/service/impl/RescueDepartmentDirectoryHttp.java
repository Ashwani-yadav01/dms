package com.dms.userService.user.service.impl;

import com.dms.userService.user.service.RescueDepartmentDirectory;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;

import java.util.UUID;

@Component
@Slf4j
public class RescueDepartmentDirectoryHttp implements RescueDepartmentDirectory {

    private final RestClient client;

    public RescueDepartmentDirectoryHttp(RestClient.Builder builder,
                                        @Value("${application.rescue-service.url:http://localhost:8083}") String baseUrl) {
        this.client = builder.baseUrl(baseUrl).build();
    }

    @Override
    @CircuitBreaker(name = "rescueDepartmentClient", fallbackMethod = "existsFallback")
    public boolean exists(UUID departmentId) {
        try {
            return client.get()
                    .uri("/api/v1/rescue/departments/{id}", departmentId)
                    .retrieve()
                    .toBodilessEntity()
                    .getStatusCode()
                    .is2xxSuccessful();
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                return false;
            }
            throw new IllegalStateException("Rescue department validation is unavailable", e);
        } catch (ResourceAccessException e) {
            throw new IllegalStateException("Rescue department validation is unavailable", e);
        }
    }

    public boolean existsFallback(UUID departmentId, Throwable ex) {
        log.warn("Circuit breaker [rescueDepartmentClient] fallback activated for department {}: {}", departmentId, ex.getMessage());
        throw new IllegalStateException("Rescue department validation service is temporarily unavailable. Please retry shortly.", ex);
    }
}
