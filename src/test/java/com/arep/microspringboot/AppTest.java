package com.arep.microspringboot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AppTest {

    @BeforeEach
    void setUp() throws Exception {
        MicroSpringBoot.loadControllers("com.arep.microspringboot");
    }

    @Test
    void shouldResolveDynamicRoute() throws Exception {
        MicroSpringBoot.HttpResponse response = MicroSpringBoot.handleRequest("GET", "/hello");

        assertEquals(200, response.statusCode());
        assertEquals("Hello World!", response.bodyAsString());
    }

    @Test
    void shouldSupportRequestParamWithDefaultValue() throws Exception {
        MicroSpringBoot.HttpResponse response = MicroSpringBoot.handleRequest("GET", "/greeting");

        assertEquals(200, response.statusCode());
        assertEquals("Hola World", response.bodyAsString());
    }

    @Test
    void shouldSupportRequestParamFromQueryString() throws Exception {
        MicroSpringBoot.HttpResponse response = MicroSpringBoot.handleRequest("GET", "/greeting?name=Jesus");

        assertEquals(200, response.statusCode());
        assertEquals("Hola Jesus", response.bodyAsString());
    }

    @Test
    void shouldServeHtmlFromClasspath() throws Exception {
        MicroSpringBoot.HttpResponse response = MicroSpringBoot.handleRequest("GET", "/index.html");

        assertEquals(200, response.statusCode());
        assertTrue(response.contentType().startsWith("text/html"));
        assertTrue(response.bodyAsString().contains("MicroSpringBoot"));
    }

    @Test
    void shouldServePngFromClasspath() throws Exception {
        MicroSpringBoot.HttpResponse response = MicroSpringBoot.handleRequest("GET", "/images/pixel.png");

        assertEquals(200, response.statusCode());
        assertEquals("image/png", response.contentType());
        assertTrue(response.body().length > 0);
    }

    @Test
    void shouldReturn404ForUnknownResource() throws Exception {
        MicroSpringBoot.HttpResponse response = MicroSpringBoot.handleRequest("GET", "/missing");

        assertEquals(404, response.statusCode());
    }
}
