package com.dms.userService.user.resilience;

import com.dms.userService.user.service.impl.RescueDepartmentDirectoryHttp;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RescueDepartmentDirectoryResilienceTest {

    @Mock
    private RestClient.Builder restClientBuilder;

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private RescueDepartmentDirectoryHttp directory;
    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() {
        when(restClientBuilder.baseUrl(anyString())).thenReturn(restClientBuilder);
        when(restClientBuilder.build()).thenReturn(restClient);

        directory = new RescueDepartmentDirectoryHttp(restClientBuilder, "http://localhost:8083");

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(4)
                .minimumNumberOfCalls(2)
                .failureRateThreshold(50.0f)
                .waitDurationInOpenState(Duration.ofMillis(200))
                .permittedNumberOfCallsInHalfOpenState(2)
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        circuitBreaker = registry.circuitBreaker("rescueDepartmentClient");
    }

    @Test
    @DisplayName("Department exists (HTTP 200) returns true and breaker remains CLOSED")
    void testDepartmentExistsSuccess() {
        UUID deptId = UUID.randomUUID();
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object.class))).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(ResponseEntity.ok().build());

        boolean exists = circuitBreaker.executeSupplier(() -> directory.exists(deptId));

        assertTrue(exists);
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
        assertEquals(1, circuitBreaker.getMetrics().getNumberOfSuccessfulCalls());
    }

    @Test
    @DisplayName("Department not found (HTTP 404) returns false without throwing exception or opening breaker")
    void testDepartmentNotFoundDoesNotTripBreaker() {
        UUID deptId = UUID.randomUUID();
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object.class))).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY, new byte[0], null));

        boolean exists = circuitBreaker.executeSupplier(() -> directory.exists(deptId));

        assertFalse(exists);
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
        assertEquals(1, circuitBreaker.getMetrics().getNumberOfSuccessfulCalls());
    }

    @Test
    @DisplayName("Network timeouts trip circuit breaker to OPEN, invoking fallback with descriptive error")
    void testNetworkTimeoutsTripBreaker() {
        UUID deptId = UUID.randomUUID();
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Object.class))).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenThrow(new ResourceAccessException("Connection refused"));

        // Call 1 fails
        assertThrows(IllegalStateException.class, () ->
                circuitBreaker.executeSupplier(() -> directory.exists(deptId)));

        // Call 2 fails -> trips breaker
        assertThrows(IllegalStateException.class, () ->
                circuitBreaker.executeSupplier(() -> directory.exists(deptId)));

        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());

        // Verify fallback method throws informative exception
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                directory.existsFallback(deptId, io.github.resilience4j.circuitbreaker.CallNotPermittedException.createCallNotPermittedException(circuitBreaker)));

        assertTrue(ex.getMessage().contains("temporarily unavailable"));
    }
}
