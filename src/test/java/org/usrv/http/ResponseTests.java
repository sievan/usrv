package org.usrv.http;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.MockedStatic;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ResponseTests {

    @Test
    @DisplayName("Responses for all relevant status codes can be created with default values inferred")
    void testStatuses() {
        String ok = new Response(200).getFullResponseHeaders();
        assertThat(ok, containsString("HTTP/1.1 200 OK"));

        String badRequest = new Response(400).getFullResponseHeaders();
        assertThat(badRequest, containsString("HTTP/1.1 400 Bad Request"));

        String notFound = new Response(404).getFullResponseHeaders();
        assertThat(notFound, containsString("HTTP/1.1 404 Not Found"));

        String serverError = new Response(500).getFullResponseHeaders();
        assertThat(serverError, containsString("HTTP/1.1 500 Internal Server Error"));
    }

    @Test
    @DisplayName("Default headers are set")
    void testHeaders() {
        String instant = "2025-02-02T10:15:30Z";
        Clock clock = Clock.fixed(Instant.parse(instant), ZoneId.of("UTC"));
        ZonedDateTime dateTime = ZonedDateTime.now(clock);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
                "EEE, dd MMM yyyy HH:mm:ss z",
                Locale.ENGLISH
        );

        String expectedDateTime = dateTime.format(formatter);
        Response response;

        try (MockedStatic<ZonedDateTime> mockedStatic = mockStatic(ZonedDateTime.class)) {
            mockedStatic.when(() -> ZonedDateTime.now(ZoneOffset.UTC)).thenReturn(dateTime);

            response = new Response(200);
            assertEquals(expectedDateTime, response.getHeaders().get("Date"));
        }

        assertEquals("usrv", response.getHeaders().get("Server"));

        assertEquals(2, response.getHeaders().size());
    }

    @Test
    @DisplayName("Response headers can be set, gotten and read from the full response")
    void testGetSetHeaders() {
        Response response = new Response(200);

        assertEquals(2, response.getHeaders().size());

        response.setHeader("Content-Type", "text/plain");
        assertEquals("text/plain", response.getHeaders().get("Content-Type"));
        assertThat(response.getFullResponseHeaders(), containsString("Content-Type: text/plain"));

        response.setHeader("X-Some-Header", "a-value");
        assertEquals("a-value", response.getHeaders().get("X-Some-Header"));
        assertThat(response.getFullResponseHeaders(), containsString("X-Some-Header: a-value"));

        assertEquals(4, response.getHeaders().size());
    }

    @Test
    @DisplayName("Binary data from an image is correctly handled in response body")
    void testImageBinaryDataHandling() throws Exception {
        Response response = new Response(200);
        response.setHeader("Content-Type", "image/jpeg");
        Path imagePath = Paths.get("src", "test", "resources", "testImage.jpg");

        byte[] imageData = Files.readAllBytes(imagePath);
        response.setBody(imageData);

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] expectedHash = digest.digest(imageData);
        byte[] responseHash = digest.digest(response.getBody());

        // Test that binary data is preserved exactly
        assertArrayEquals(expectedHash, responseHash, "Binary data should be preserved without modification");

        // Test that appropriate headers are set
        assertEquals("image/jpeg", response.getHeaders().get("Content-Type"));
    }

    @Test
    @DisplayName("Binary data from a text file is correctly handled in response body")
    void testTextBinaryDataHandling() throws Exception {
        Response response = new Response(200);
        response.setHeader("Content-Type", "text/html");
        Path imagePath = Paths.get("src", "test", "resources", "testPage.html");

        byte[] imageData = Files.readAllBytes(imagePath);
        response.setBody(imageData);

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] expectedHash = digest.digest(imageData);
        byte[] responseHash = digest.digest(response.getBody());

        // Test that binary data is preserved exactly
        assertArrayEquals(expectedHash, responseHash, "Binary data should be preserved without modification");

        // Test that appropriate headers are set
        assertEquals("text/html", response.getHeaders().get("Content-Type"));
    }
}