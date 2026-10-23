package com.dms.gateway;

import com.dms.gateway.controller.FallbackController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FallbackControllerTest {

    private FallbackController controller;

    @BeforeEach
    void setUp() {
        controller = new FallbackController();
    }

    @Test
    @DisplayName("serviceFallback returns 503 with structured payload")
    void testServiceFallback() {
        ResponseEntity<Map<String, Object>> response = controller.serviceFallback("user-service").block();

        assertNotNull(response);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());

        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(503, body.get("status"));
        assertEquals("Service Unavailable", body.get("error"));
        assertTrue(body.get("message").toString().contains("user-service"));
        assertNotNull(body.get("timestamp"));
    }

    @Test
    @DisplayName("defaultFallback delegates to serviceFallback with downstream-service")
    void testDefaultFallback() {
        ResponseEntity<Map<String, Object>> response = controller.defaultFallback().block();

        assertNotNull(response);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());

        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue(body.get("message").toString().contains("downstream-service"));
    }
}
