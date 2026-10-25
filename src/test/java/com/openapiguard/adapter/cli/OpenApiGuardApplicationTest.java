package com.openapiguard.adapter.cli;

import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Contexto Spring Boot no web, ensamblado y cierre controlado. */
class OpenApiGuardApplicationTest {

    @Test
    void startsWithoutWebServerAndClosesContext() {
        ConfigurableApplicationContext context = OpenApiGuardApplication.start();

        try (context) {
            assertTrue(context.isActive());
            assertInstanceOf(AnnotationConfigApplicationContext.class, context);
            assertNotNull(context.getBean(OpenApiGuardApplication.class));
        }

        assertFalse(context.isActive());
    }
}
