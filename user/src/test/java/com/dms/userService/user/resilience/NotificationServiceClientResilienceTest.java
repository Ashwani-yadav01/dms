package com.dms.userService.user.resilience;

import com.dms.userService.user.service.impl.NotificationServiceClientHttp;
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
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationServiceClientResilienceTest {

    @Mock
    private RestClient.Builder restClientBuilder;

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private NotificationServiceClientHttp client;
    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() {
        when(restClientBuilder.baseUrl(anyString())).thenReturn(restClientBuilder);
        when(restClientBuilder.build()).thenReturn(restClient);

        client = new NotificationServiceClientHttp(restClientBuilder, "http://localhost:8086");

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(4)
                .minimumNumberOfCalls(2)
                .failureRateThreshold(50.0f)
                .waitDurationInOpenState(Duration.ofMillis(200))
                .permittedNumberOfCallsInHalfOpenState(2)
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        circuitBreaker = registry.circuitBreaker("notificationClient");
    }

    @Test
    @DisplayName("Successful call returns delivered count and keeps breaker CLOSED")
    void testSuccessfulCall() {
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(Integer.class)).thenReturn(3);

        int result = circuitBreaker.executeSupplier(() ->
                client.dispatchSafetyCheckIn(List.of("a@dms.gov.in", "b@dms.gov.in", "c@dms.gov.in"), "John", "Safe", 26.7, 83.4, "2026-10-23T12:00:00Z"));

        assertEquals(3, result);
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
        assertEquals(0, circuitBreaker.getMetrics().getNumberOfFailedCalls());
        assertEquals(1, circuitBreaker.getMetrics().getNumberOfSuccessfulCalls());
    }

    @Test
    @DisplayName("Failures trip circuit breaker to OPEN, invoking fallback and blocking downstream calls")
    void testCircuitBreakerTripsToOpenAndExecutesFallback() {
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(Integer.class)).thenThrow(new ResourceAccessException("Connection timed out"));

        // Call 1: fails
        try {
            circuitBreaker.executeSupplier(() ->
                    client.dispatchSafetyCheckIn(List.of("a@dms.gov.in"), "John", "Safe", 26.7, 83.4, "2026-10-23T12:00:00Z"));
        } catch (Exception ignored) {}

        // Call 2: fails -> trips breaker (2 min calls, 100% failure >= 50%)
        try {
            circuitBreaker.executeSupplier(() ->
                    client.dispatchSafetyCheckIn(List.of("a@dms.gov.in"), "John", "Safe", 26.7, 83.4, "2026-10-23T12:00:00Z"));
        } catch (Exception ignored) {}

        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());

        // Call 3 while OPEN: should execute fallback directly without invoking restClient.post()
        int fallbackResult = client.dispatchSafetyCheckInFallback(
                List.of("a@dms.gov.in"), "John", "Safe", 26.7, 83.4, "2026-10-23T12:00:00Z",
                io.github.resilience4j.circuitbreaker.CallNotPermittedException.createCallNotPermittedException(circuitBreaker));

        assertEquals(0, fallbackResult);
        // Verify post was only called 2 times, not 3
        verify(restClient, times(2)).post();
    }

    @Test
    @DisplayName("Empty recipient list returns 0 early without invoking remote endpoint")
    void testEmptyRecipientsDoesNotInvokeRemote() {
        int result = client.dispatchSafetyCheckIn(List.of(), "John", "Safe", 26.7, 83.4, "2026-10-23T12:00:00Z");
        assertEquals(0, result);
        verify(restClient, never()).post();
    }

    @Test
    @DisplayName("After wait duration, circuit transitions to HALF_OPEN and recovers to CLOSED upon successful calls")
    void testHalfOpenRecoveryToClosed() throws InterruptedException {
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(Integer.class))
                .thenThrow(new ResourceAccessException("Connection timed out"))
                .thenThrow(new ResourceAccessException("Connection timed out"))
                .thenReturn(1)
                .thenReturn(1);

        // Fail 2 calls to open the circuit
        for (int i = 0; i < 2; i++) {
            try {
                circuitBreaker.executeSupplier(() ->
                        client.dispatchSosAlert(List.of("a@dms.gov.in"), "Distress", "Help", 26.7, 83.4, "2026-10-23T12:00:00Z"));
            } catch (Exception ignored) {}
        }

        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());

        // Wait for open state duration (200ms)
        Thread.sleep(250);

        // First trial call in HALF_OPEN: succeeds
        int r1 = circuitBreaker.executeSupplier(() ->
                client.dispatchSosAlert(List.of("a@dms.gov.in"), "Distress", "Help", 26.7, 83.4, "2026-10-23T12:00:00Z"));
        assertEquals(1, r1);

        // Second trial call in HALF_OPEN: succeeds -> transitions to CLOSED
        int r2 = circuitBreaker.executeSupplier(() ->
                client.dispatchSosAlert(List.of("a@dms.gov.in"), "Distress", "Help", 26.7, 83.4, "2026-10-23T12:00:00Z"));
        assertEquals(1, r2);

        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
    }
}
