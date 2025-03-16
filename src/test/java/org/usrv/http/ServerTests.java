package org.usrv.http;

import org.junit.jupiter.api.*;
import org.usrv.config.ServerConfig;

import java.io.*;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpHeaders;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;

record ServerAndThread(Server server, Thread thread) {
}

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ServerTests {
    private Server server;
    private Thread serverThread;
    private HttpClient httpClient;
    private static final String TEST_CONTENT = "<html><body>Test Content</body></html>";
    private static final Path defaultDistDirectory = Path.of("./TEST_DIST/dist");

    ServerAndThread startServerInNewThread(ServerConfig config) {
        Thread thread;
        Server newServer = new Server(config);
        thread = new Thread(newServer::start);
        thread.start();

        return new ServerAndThread(newServer, thread);
    }


    @BeforeAll
    void setup() throws Exception {
        // Create test dist directory and index.html
        Files.createDirectories(defaultDistDirectory);
        Files.writeString(Path.of(defaultDistDirectory.toString(), "index.html"), TEST_CONTENT);

        // Initialize HTTP client
        System.setProperty("jdk.httpclient.allowRestrictedHeaders", "Connection");
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        // Start server in separate thread
        ServerConfig config = new ServerConfig(defaultDistDirectory.toString(), 80, false);
        ServerAndThread serverAndThread = startServerInNewThread(config);
        server = serverAndThread.server();
        serverThread = serverAndThread.thread();

        // Wait for server to start
        Thread.sleep(1000);
    }

    /*
     * Integration tests
     */
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
    @DisplayName("Server can be started with custom config")
    void serverWithCustomConfig() throws Exception {
        ServerAndThread customServerAndThread = startServerInNewThread(new ServerConfig(defaultDistDirectory.toString(), 81, false));

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:81"))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            assertEquals(200, response.statusCode());
            assertThat(response.body(), containsString(TEST_CONTENT));
        } finally {
            customServerAndThread.server().stop();
            customServerAndThread.thread().join(1000);
        }
    }

    @Test
    @DisplayName("Server can respond with an image")
    public void testImageEndpoint() throws IOException, InterruptedException, NoSuchAlgorithmException {
        Path imagePath = Paths.get("src", "test", "resources", "testImage.jpg");
        Files.copy(imagePath, defaultDistDirectory.resolve("testImage.jpg"));

        // Build request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:80/testImage.jpg"))
                .GET()
                .build();

        // Get response bytes
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        byte[] responseBytes = response.body();

        // Calculate hash of response
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] responseHash = digest.digest(responseBytes);

        // Reset digest for reuse
        digest.reset();

        // Calculate hash of expected file
        byte[] expectedFileBytes = Files.readAllBytes(imagePath);
        byte[] expectedHash = digest.digest(expectedFileBytes);

        // Compare hashes
        assertArrayEquals(expectedHash, responseHash, "Image file hash doesn't match expected hash");
    }

    @Test
    @DisplayName("A server can be run with SPA configuration")
    void serverWithSPAConfig() throws Exception {
        ServerAndThread customServerAndThread = startServerInNewThread(new ServerConfig(defaultDistDirectory.toString(), 82, true));

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:82"))
                    .header("Accept", "text/html")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            assertEquals(200, response.statusCode());
            assertThat(response.body(), containsString(TEST_CONTENT));

            assertTrue(customServerAndThread.server().isShouldRun());

            HttpRequest requestSubpage = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:82/asubpage"))
                    .header("Accept", "text/html")
                    .GET()
                    .build();

            HttpResponse<String> responseSubpage = httpClient.send(requestSubpage,
                    HttpResponse.BodyHandlers.ofString());

            assertEquals(200, responseSubpage.statusCode());
            assertThat(responseSubpage.body(), containsString(TEST_CONTENT));
        } finally {
            customServerAndThread.server().stop();
            customServerAndThread.thread().join(1000);
        }
    }

    @Test
    @DisplayName("Server responds with a 400 status if the request is malformed")
    void serverRespondsWith400() throws Exception {
        try (Socket socket = new Socket("localhost", 80);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Send a malformed request
            out.println("GET");
            out.println("Host: localhost");
            out.println("User-Agent: insomnia/10.3.1\n");
            out.println("Accept: */*\n");  // Missing path and HTTP version
            out.flush();

            // Read and print the response
            ArrayList<String> lines = new ArrayList<>();
            String line;

            while ((line = in.readLine()) != null && !line.isEmpty()) {
                lines.add(line);
            }


            assertEquals("HTTP/1.1 400 Bad Request", lines.getFirst());
            String fullResponse = String.join("\n", lines);
            assertThat(fullResponse, containsString("Connection: close"));
        }
    }

    @Test
    @DisplayName("Server responds with a 400 status if the request is missing host for HTTP 1.1")
    void serverRespondsWith400WhenMissingHost() throws Exception {
        try (Socket socket = new Socket("localhost", 80);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("GET / HTTP/1.1");
            out.println("User-Agent: insomnia/10.3.1\n");
            out.println("Accept: */*\n");
            out.flush();

            ArrayList<String> lines = new ArrayList<>();
            String line;

            while ((line = in.readLine()) != null && !line.isEmpty()) {
                lines.add(line);
            }

            assertEquals("HTTP/1.1 400 Bad Request", lines.getFirst());

            String fullResponse = String.join("\n", lines);
            assertThat(fullResponse, containsString("Connection: close"));
        }
    }

    @Test
    @DisplayName("Server uses cached response for subsequent requests")
    void serverUsesCachedResponse() throws Exception {
        Path path = Path.of("./TEST_DIST/dist/index.html");
        try {
            // Make first request to populate cache
            HttpRequest firstRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:80"))
                    .GET()
                    .build();

            httpClient.send(firstRequest, HttpResponse.BodyHandlers.ofString());

            // Modify the file content
            Files.writeString(path, "Modified Content");

            // Make second request - should get cached response
            HttpRequest secondRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:80"))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(secondRequest,
                    HttpResponse.BodyHandlers.ofString());

            assertTrue(response.body().contains(TEST_CONTENT));
            assertFalse(response.body().contains("Modified Content"));
        } finally {
            Files.writeString(path, TEST_CONTENT);
        }
    }

    @Test
    @DisplayName("Server should not cache the keep alive header")
    void serverDoesntCacheKeepAlive() throws Exception {
        Path path = Path.of("./TEST_DIST/dist/index.html");

        try {
            // Make first request to populate cache
            HttpRequest firstRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:80"))
                    .header("Connection", "keep-alive")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(firstRequest, HttpResponse.BodyHandlers.ofString());

            String connectionHeader = response.headers().map().get("Connection").getFirst();
            assertEquals("keep-alive", connectionHeader);

            // Make second request - should not get cached header
            HttpRequest secondRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:80"))
                    .header("Connection", "close")
                    .GET()
                    .build();

            response = httpClient.send(secondRequest,
                    HttpResponse.BodyHandlers.ofString());

            connectionHeader = response.headers().map().get("Connection").getFirst();
            assertEquals("close", connectionHeader);
        } finally {
            Files.writeString(path, TEST_CONTENT);
        }
    }

    @Test
    @DisplayName("Server should respond with the requested file and contents")
    void serverRespondsWithAnyFile() throws Exception {
        Files.writeString(Path.of(defaultDistDirectory.toString(), "newfile.txt"), "New content");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:80/newfile.txt"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertThat(response.body(), containsString("New content"));
    }

    @Test
    @DisplayName("Server handles different mime types correctly")
    void serverRespondsWithRightMimeType() throws Exception {
        // text/plain
        Files.writeString(Path.of(defaultDistDirectory.toString(), "newfile.txt"), "New content");
        HttpRequest txtRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:80/newfile.txt"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(txtRequest,
                HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("text/plain", response.headers().map().get("Content-Type").getFirst());
        assertThat(response.body(), containsString("New content"));

        // image/svg+xml
        Files.writeString(Path.of(defaultDistDirectory.toString(), "newfile.svg"), "New content");
        HttpRequest svgRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:80/newfile.svg"))
                .GET()
                .build();

        response = httpClient.send(svgRequest,
                HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("image/svg+xml", response.headers().map().get("Content-Type").getFirst());
        assertThat(response.body(), containsString("New content"));
    }

    @Test
    @DisplayName("Server responds with 404 if file isn't found")
    void serverRespondsWith404() throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:80/notafile"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
    }

    @Test
    @DisplayName("Server handles multiple concurrent requests")
    void serverHandlesMultipleRequests() throws Exception {
        // Create HttpClients with requests running in their own threads
        try (HttpClient multiThreadedHttpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .executor(Executors.newVirtualThreadPerTaskExecutor()) // Use virtual threads for client
                .build()) {

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:80"))
                    .GET()
                    .build();

            // Send 1000 concurrent requests
            var futures = new ArrayList<CompletableFuture<HttpResponse<String>>>();
            for (int i = 0; i < 1000; i++) {
                futures.add(multiThreadedHttpClient.sendAsync(request,
                        HttpResponse.BodyHandlers.ofString()));
            }

            // Wait for all requests to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(60, TimeUnit.SECONDS);

            // Verify all responses
            for (var future : futures) {
                HttpResponse<String> response = future.get();
                assertEquals(200, response.statusCode());
                assertThat(response.body(), containsString(TEST_CONTENT));
            }
        }
    }

    @Test
    @DisplayName("Server should respond with headers for HEAD requests")
    void serverRespondsWithHeadersForFile() throws Exception {
        Files.writeString(Path.of(defaultDistDirectory.toString(), "newfile.txt"), "New content");

        try (Socket socket = new Socket("localhost", 80);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Send HEAD request
            out.println("HEAD /newfile.txt HTTP/1.1");
            out.println("Host: localhost");
            out.println("");

            String statusLine = in.readLine();
            assertNotNull(statusLine, "Status line should not be null");
            assertTrue(statusLine.startsWith("HTTP/1.1 200"), "Expected HTTP 200 response");

            String header;
            while ((header = in.readLine()) != null && !header.isEmpty()) {
                // Go through headers, but don't check them here
                System.out.println(header);
            }

            // Check if body is returned
            assertEquals(0, socket.getInputStream().available(), "HEAD response should not contain a body");
        }
    }

    @Test
    @DisplayName("Server should respond with 'Connection: keep-alive' in the header")
    void testKeepAliveHeader() throws IOException {
        Files.writeString(Path.of(defaultDistDirectory.toString(), "timeout.txt"), "Timeout test");

        try (Socket socket = new Socket("localhost", 80);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.print("GET /timeout.txt HTTP/1.1\r\n");
            out.print("Host: localhost\r\n");
            out.print("User-Agent: insomnia/10.3.1\r\n");
            out.print("Connection: keep-alive\r\n");
            out.print("\r\n");
            out.flush();

            String statusLine = in.readLine();
            assertTrue(statusLine.startsWith("HTTP/1.1 200"), "Expected HTTP 200 response");

            boolean keepAliveFound = false;
            String line;
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                if (line.equalsIgnoreCase("Connection: keep-alive")) {
                    keepAliveFound = true;
                }
            }

            assertTrue(keepAliveFound, "Server did not return 'Connection: keep-alive'");
        }
    }

    @Test
    @DisplayName("Server should respond with 'Connection: close' in the header")
    void testCloseConnectionHeader() throws IOException, InterruptedException {
        Files.writeString(Path.of(defaultDistDirectory.toString(), "file.txt"), "Keep-alive test");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:80/file.txt"))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode(), "Expected HTTP 200 response");

        HttpHeaders headers = response.headers();
        String connectionHeader = headers.firstValue("Connection").orElse("");
        assertEquals("close", connectionHeader, "Server did not return 'Connection: close'");
    }

    @Test
    @DisplayName("Server should keep connection alive for multiple requests if the client requests it")
    void testKeepAliveConnection() throws IOException {
        Files.writeString(Path.of(defaultDistDirectory.toString(), "file.txt"), "File test");

        try (Socket socket = new Socket("localhost", 80);
             PrintStream out = new PrintStream(socket.getOutputStream(), true);
             InputStream in = socket.getInputStream()) {

            // Send first request with keep-alive
            out.print("GET /file.txt HTTP/1.1\r\n");
            out.print("Host: localhost\r\n");
            out.print("User-Agent: insomnia/10.3.1\r\n");
            out.print("Connection: keep-alive\r\n");
            out.print("\r\n");
            out.flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String statusLine1 = reader.readLine();

            assertNotNull(statusLine1, "Status line should not be null");
            assertTrue(statusLine1.startsWith("HTTP/1.1 200"), "Expected HTTP 200 response");

            String line;
            int contentLength = -1;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                if (line.toLowerCase().startsWith("content-length:")) {
                    contentLength = Integer.parseInt(line.split(": ")[1].trim());
                }
            }

            int counter = 0;
            while (counter < contentLength) {
                //noinspection ResultOfMethodCallIgnored
                reader.read();
                counter++;
            }

            out.print("GET /file.txt HTTP/1.1\r\n");
            out.print("Host: localhost\r\n");
            out.print("User-Agent: insomnia/10.3.1\r\n");
            out.print("Connection: close\r\n");
            out.print("\r\n");
            out.flush();


            String statusLine2 = reader.readLine();
            assertNotNull(statusLine2, "Status line should not be null");
            assertTrue(statusLine2.startsWith("HTTP/1.1 200"), "Expected HTTP 200 response");
        }
    }

    @Test
    @DisplayName("Server should close connection after receiving 'Connection: close'")
    void testCloseConnection() throws Exception {

        Files.writeString(Path.of(defaultDistDirectory.toString(), "file.txt"), "Keep-alive test");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:80/file.txt"))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(200, response.statusCode(), "Expected HTTP 200 response");

        HttpHeaders headers = response.headers();
        String connectionHeader = headers.firstValue("Connection").orElse("");
        assertEquals("close", connectionHeader, "Server did not return 'Connection: close'");

        httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    @DisplayName("Server always returns a content-length header in response")
    void testContentLengthHeader() throws Exception {

        // call API that will return contents of test file
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:80"))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());
        System.out.println(response);
        // Validate that Content-Length matches response body size
        //noinspection OptionalGetWithoutIsPresent
        assertEquals(String.valueOf(response.body().getBytes().length), response.headers().firstValue("Content-Length").get());
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
        try (Stream<Path> stream = Files.walk(Path.of("./TEST_DIST"))) {
            //noinspection ResultOfMethodCallIgnored
            stream.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            throw new IOException("Error while cleaning up test directory: " + e.getMessage(), e);
        }
    }
}
