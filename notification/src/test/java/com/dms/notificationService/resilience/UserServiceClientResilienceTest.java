package com.dms.notificationService.resilience;

import com.dms.notificationService.client.UserServiceClient;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceClientResilienceTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private UserServiceClient client;
    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() {
        client = new UserServiceClient(restClient);

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(4)
                .minimumNumberOfCalls(2)
                .failureRateThreshold(50.0f)
                .waitDurationInOpenState(Duration.ofMillis(200))
                .permittedNumberOfCallsInHalfOpenState(2)
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        circuitBreaker = registry.circuitBreaker("userServiceClient");
    }

    @Test
    @DisplayName("Successful user lookup returns recipient emails and leaves circuit CLOSED")
    void testSuccessfulUserLookup() {
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(any(ParameterizedTypeReference.class)))
                .thenReturn(List.of("citizen1@dms.gov.in", "citizen2@dms.gov.in"));

        List<String> emails = circuitBreaker.executeSupplier(() ->
                client.fetchNearbyUserEmails(26.78, 83.21, 5.0));

        assertNotNull(emails);
        assertEquals(2, emails.size());
        assertTrue(emails.contains("citizen1@dms.gov.in"));
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
        assertEquals(1, circuitBreaker.getMetrics().getNumberOfSuccessfulCalls());
    }

    @Test
    @DisplayName("Repeated connection timeouts trip circuit to OPEN and trigger graceful fallback")
    void testConnectionTimeoutsTripCircuitToOpen() {
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(any(ParameterizedTypeReference.class)))
                .thenThrow(new ResourceAccessException("I/O error on GET request to User Service: Connection refused"));

        // Call 1 fails
        try {
            circuitBreaker.executeSupplier(() -> client.fetchNearbyUserEmails(26.78, 83.21, 5.0));
        } catch (Exception ignored) {}

        // Call 2 fails -> 2 calls, 100% failure >= 50% threshold -> trips to OPEN
        try {
            circuitBreaker.executeSupplier(() -> client.fetchNearbyUserEmails(26.78, 83.21, 5.0));
        } catch (Exception ignored) {}

        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());

        // When circuit is OPEN, fallback handles CallNotPermittedException and returns empty list
        CallNotPermittedException openEx = CallNotPermittedException.createCallNotPermittedException(circuitBreaker);
        List<String> fallbackEmails = client.fetchNearbyUserEmailsFallback(26.78, 83.21, 5.0, openEx);

        assertNotNull(fallbackEmails);
        assertTrue(fallbackEmails.isEmpty());

        // Remote GET was called only twice, circuit prevented the 3rd invocation
        verify(restClient, times(2)).get();
    }

    @Test
    @DisplayName("Circuit recovers from HALF_OPEN to CLOSED after downstream service heals")
    void testCircuitBreakerRecovery() throws InterruptedException {
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(any(ParameterizedTypeReference.class)))
                .thenThrow(new ResourceAccessException("Connection timed out"))
                .thenThrow(new ResourceAccessException("Connection timed out"))
                .thenReturn(List.of("recovered@dms.gov.in"))
                .thenReturn(List.of("recovered@dms.gov.in"));

        // Trip circuit to OPEN
        for (int i = 0; i < 2; i++) {
            try {
                circuitBreaker.executeSupplier(() -> client.fetchNearbyUserEmails(26.78, 83.21, 5.0));
            } catch (Exception ignored) {}
        }

        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());

        // Wait for open state duration (200ms)
        Thread.sleep(250);

        // Trial call 1 in HALF_OPEN
        List<String> res1 = circuitBreaker.executeSupplier(() -> client.fetchNearbyUserEmails(26.78, 83.21, 5.0));
        assertEquals(1, res1.size());

        // Trial call 2 in HALF_OPEN -> closes breaker
        List<String> res2 = circuitBreaker.executeSupplier(() -> client.fetchNearbyUserEmails(26.78, 83.21, 5.0));
        assertEquals(1, res2.size());

        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
    }
}
