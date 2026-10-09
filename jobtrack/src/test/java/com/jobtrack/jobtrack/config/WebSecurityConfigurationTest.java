package com.jobtrack.jobtrack.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebSecurityConfigurationTest {

    private final CorsConfigurationSource source = new WebSecurityConfiguration().corsConfigurationSource();

    @Test
    void corsConfigurationAllowsBothLocalFrontendOriginsAndRequiredHeadersAndMethods() {
        CorsConfiguration configuration = configurationFor("http://127.0.0.1:5500");

        assertNotNull(configuration);
        assertEquals("http://127.0.0.1:5500", configuration.checkOrigin("http://127.0.0.1:5500"));
        assertEquals("http://localhost:5500", configuration.checkOrigin("http://localhost:5500"));
        assertNull(configuration.checkOrigin("http://example.com"));
        assertEquals(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"),
                configuration.getAllowedMethods());
        assertEquals(List.of("Content-Type", "Accept"), configuration.checkHeaders(List.of("Content-Type", "Accept")));
    }

    @Test
    void corsConfigurationIsRegisteredForApiPreflightRequests() {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/applications");
        request.addHeader("Origin", "http://localhost:5500");
        request.addHeader("Access-Control-Request-Method", "PATCH");

        CorsConfiguration configuration = source.getCorsConfiguration(request);

        assertNotNull(configuration);
        assertEquals("http://localhost:5500", configuration.checkOrigin("http://localhost:5500"));
        assertTrue(configuration.checkHttpMethod(HttpMethod.PATCH).contains(HttpMethod.PATCH));
    }

    private CorsConfiguration configurationFor(String origin) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/applications");
        request.addHeader("Origin", origin);
        return source.getCorsConfiguration(request);
    }
}
