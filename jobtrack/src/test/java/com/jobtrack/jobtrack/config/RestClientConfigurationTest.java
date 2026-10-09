package com.jobtrack.jobtrack.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class RestClientConfigurationTest {

    @Test
    void providesRestClientBuilderBean() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(RestClientConfiguration.class)) {
            assertNotNull(context.getBean(RestClient.Builder.class));
        }
    }
}
