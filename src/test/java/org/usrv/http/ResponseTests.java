package org.usrv.http;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.MockedStatic;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ResponseTests {

    @Test
    @DisplayName("A 200 response can be created with default values inferred")
    void testBasicHttpResponse() {
        Response response = new Response(200);
        response.setBody("<html>Hello</html>".getBytes(StandardCharsets.UTF_8));

        String responseText = response.toString();

        assertTrue(responseText.contains("HTTP/1.1 200 OK"));
        assertTrue(responseText.contains("<html>Hello</html>"));
    }

    @Test
    @DisplayName("A 200 response can be created with image request")
    void testImageHttpResponse() throws IOException, NoSuchAlgorithmException {
        Response response = new Response(200);
        response.setHeader("Content-Type", "image/jpeg");
        Path imagePath = Paths.get("src", "test", "resources", "testImage.jpg");

        byte[] image = Files.readAllBytes(imagePath);
        response.setBody(image);
        String responseText = response.toString();
        String responseBody = responseText.splitWithDelimiters("\n\n", 2)[2];


        byte[] responseBytes = Base64.getDecoder().decode(responseBody);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] responseHash = digest.digest(responseBytes);
        digest.reset();
        byte[] expectedHash = digest.digest(image);

        assertTrue(responseText.contains("HTTP/1.1 200 OK"));
        assertArrayEquals(expectedHash, responseHash, "Image file hash doesn't match expected hash");
    }

    @Test
    @DisplayName("Responses for all relevant status codes can be created with default values inferred")
    void testStatuses() {
        Response ok = new Response(200);
        assertThat(ok.toString(), containsString("HTTP/1.1 200 OK"));

        Response badRequest = new Response(400);
        assertThat(badRequest.toString(), containsString("HTTP/1.1 400 Bad Request"));

        Response notFound = new Response(404);
        assertThat(notFound.toString(), containsString("HTTP/1.1 404 Not Found"));

        Response serverError = new Response(500);
        assertThat(serverError.toString(), containsString("HTTP/1.1 500 Internal Server Error"));
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
            ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);

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
        assertThat(response.toString(), containsString("Content-Type: text/plain"));

        response.setHeader("X-Some-Header", "a-value");
        assertEquals("a-value", response.getHeaders().get("X-Some-Header"));
        assertThat(response.toString(), containsString("X-Some-Header: a-value"));

        assertEquals(4, response.getHeaders().size());
    }
}