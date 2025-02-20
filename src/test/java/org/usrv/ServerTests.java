package org.usrv;

import org.junit.jupiter.api.*;
import org.usrv.Server;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ServerTests {
    private Server server;
    private Thread serverThread;
    private HttpClient httpClient;
    private static final String TEST_CONTENT = "<html><body>Test Content</body></html>";
    private static final Path defaultDistDirectory = Path.of("./TEST/dist");

    @BeforeAll
    void setup() throws Exception {
        // Create test dist directory and index.html
        Files.createDirectories(defaultDistDirectory);
        Files.writeString(Path.of(defaultDistDirectory.toString(), "index.html"), TEST_CONTENT);

        // Initialize HTTP client
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        // Start server in separate thread
        serverThread = new Thread(() -> {
            server = new Server(defaultDistDirectory.toString());
        });
        serverThread.start();

        // Wait for server to start
        Thread.sleep(1000);
    }

    @Test
    @DisplayName("Server responds to HTTP GET request")
    void serverRespondsToHttpRequest() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:80"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertThat(response.body(), containsString(TEST_CONTENT));
    }

    @Test
    @DisplayName("Server uses cached response for subsequent requests")
    void serverUsesCachedResponse() throws Exception {
        // Make first request to populate cache
        HttpRequest firstRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:80"))
                .GET()
                .build();

        httpClient.send(firstRequest, HttpResponse.BodyHandlers.ofString());

        // Modify the file content
        Files.writeString(Path.of("./TEST/dist/index.html"), "Modified Content");

        // Make second request - should get cached response
        HttpRequest secondRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:80"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(secondRequest,
                HttpResponse.BodyHandlers.ofString());

        assertTrue(response.body().contains(TEST_CONTENT));
        assertFalse(response.body().contains("Modified Content"));
    }

    @Test
    @DisplayName("Server handles multiple concurrent requests")
    void serverHandlesMultipleRequests() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:80"))
                .GET()
                .build();

        // Send 5 concurrent requests
        var futures = new ArrayList<CompletableFuture<HttpResponse<String>>>();
        for (int i = 0; i < 5; i++) {
            futures.add(httpClient.sendAsync(request,
                    HttpResponse.BodyHandlers.ofString()));
        }

        // Wait for all requests to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .get(10, TimeUnit.SECONDS);

        // Verify all responses
        for (var future : futures) {
            HttpResponse<String> response = future.get();
            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains(TEST_CONTENT));
        }
    }

    @AfterAll
    void cleanup() throws Exception {
        // Stop server
        if (server != null) {
            server.setShouldRun(false);
        }
        if (serverThread != null) {
            serverThread.join(1000); // Wait up to 5 seconds for server to stop
        }

        // Clean up test files

        try (Stream<Path> stream = Files.walk(Path.of("./TEST"))) {
            stream.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            throw new IOException("Error while cleaning up test directory: " + e.getMessage(), e);
        }
    }
}
