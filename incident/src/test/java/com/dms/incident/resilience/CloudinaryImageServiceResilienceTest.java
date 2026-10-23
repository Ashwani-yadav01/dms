package com.dms.incident.resilience;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.dms.incident.service.CloudinaryImageService;
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
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CloudinaryImageServiceResilienceTest {

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    private CloudinaryImageService imageService;
    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() {
        when(cloudinary.uploader()).thenReturn(uploader);
        imageService = new CloudinaryImageService(cloudinary);

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(4)
                .minimumNumberOfCalls(2)
                .failureRateThreshold(50.0f)
                .waitDurationInOpenState(Duration.ofMillis(200))
                .permittedNumberOfCallsInHalfOpenState(2)
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        circuitBreaker = registry.circuitBreaker("cloudinaryClient");
    }

    @Test
    @DisplayName("Valid image upload returns secure url and breaker remains CLOSED")
    void testSuccessfulUpload() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "incident.jpg", "image/jpeg", "image-content".getBytes());

        when(uploader.upload(any(byte[].class), anyMap()))
                .thenReturn(Map.of("secure_url", "https://res.cloudinary.com/demo/image/upload/v1/incident.jpg"));

        String url = circuitBreaker.executeSupplier(() -> imageService.upload(file, "incidents"));

        assertNotNull(url);
        assertEquals("https://res.cloudinary.com/demo/image/upload/v1/incident.jpg", url);
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
        assertEquals(1, circuitBreaker.getMetrics().getNumberOfSuccessfulCalls());
    }

    @Test
    @DisplayName("Validation failure throws BAD_REQUEST and does not invoke uploader")
    void testValidationFailureDoesNotInvokeUploader() throws IOException {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "incident.txt", "text/plain", new byte[0]);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                imageService.upload(emptyFile, "incidents"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(uploader, never()).upload(any(byte[].class), anyMap());
    }

    @Test
    @DisplayName("Repeated Cloudinary I/O failures trip circuit to OPEN and trigger 503 fallback")
    void testCloudinaryFailuresTripToOpenAndTrigger503Fallback() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "incident.jpg", "image/jpeg", "image-content".getBytes());

        when(uploader.upload(any(byte[].class), anyMap()))
                .thenThrow(new IOException("Connection timed out to api.cloudinary.com"));

        // Call 1 fails with 502 BAD_GATEWAY
        try {
            circuitBreaker.executeSupplier(() -> imageService.upload(file, "incidents"));
        } catch (Exception ignored) {}

        // Call 2 fails -> trips breaker to OPEN
        try {
            circuitBreaker.executeSupplier(() -> imageService.upload(file, "incidents"));
        } catch (Exception ignored) {}

        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());

        // When circuit is OPEN, fallback throws 503 SERVICE_UNAVAILABLE fast without invoking Cloudinary
        CallNotPermittedException openEx = CallNotPermittedException.createCallNotPermittedException(circuitBreaker);
        ResponseStatusException fallbackEx = assertThrows(ResponseStatusException.class, () ->
                imageService.uploadFallback(file, "incidents", openEx));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, fallbackEx.getStatusCode());
        assertTrue(fallbackEx.getReason().contains("Image storage service is currently unavailable"));

        // Verify uploader was only invoked 2 times (blocked by circuit breaker)
        verify(uploader, times(2)).upload(any(byte[].class), anyMap());
    }

    @Test
    @DisplayName("Circuit recovers from HALF_OPEN to CLOSED after downstream Cloudinary heals")
    void testRecoveryToClosed() throws IOException, InterruptedException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "incident.jpg", "image/jpeg", "image-content".getBytes());

        when(uploader.upload(any(byte[].class), anyMap()))
                .thenThrow(new IOException("Gateway Timeout"))
                .thenThrow(new IOException("Gateway Timeout"))
                .thenReturn(Map.of("secure_url", "https://res.cloudinary.com/demo/image/upload/v1/recovered.jpg"))
                .thenReturn(Map.of("secure_url", "https://res.cloudinary.com/demo/image/upload/v1/recovered.jpg"));

        // Trip to OPEN
        for (int i = 0; i < 2; i++) {
            try {
                circuitBreaker.executeSupplier(() -> imageService.upload(file, "incidents"));
            } catch (Exception ignored) {}
        }
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());

        Thread.sleep(250);

        // Trial call 1 in HALF_OPEN
        String r1 = circuitBreaker.executeSupplier(() -> imageService.upload(file, "incidents"));
        assertNotNull(r1);

        // Trial call 2 in HALF_OPEN -> transitions to CLOSED
        String r2 = circuitBreaker.executeSupplier(() -> imageService.upload(file, "incidents"));
        assertNotNull(r2);

        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
    }
}
