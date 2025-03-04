package org.usrv.http;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;

import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.usrv.config.ServerConfig;
import org.usrv.file.StaticFile;
import org.usrv.exceptions.RequestParsingException;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

public class Server {
    public final int port;

    @Setter
    @Getter
    private boolean shouldRun = true;

    private final String distFolder;

    private final ServerConfig serverConfig;

    private final Map<Path, Response> cache = new ConcurrentHashMap<>();

    private final static Logger logger = LoggerFactory.getLogger(Server.class);

    public Server() {
        this(ServerConfig.getDefaultConfig());
    }

    public Server(ServerConfig config) {

        this.serverConfig = config;
        this.distFolder = config.distFolder();
        this.port = config.port();
    }

    public void start() {
        try (ServerSocket socket = new ServerSocket(port, 1000)) {
            System.out.printf("Server started at port: %s%n", port);

            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                while (shouldRun) {
                    Socket clientSocket = socket.accept();
                    clientSocket.setSoTimeout(30000);
                    executor.submit(() -> handleRequest(clientSocket));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private volatile boolean shutdownRequested = false;
    
    public void stop() {
        shouldRun = false;
        shutdownRequested = true;
    }

    private void handleRequest(Socket socket) {
        String requestId = UUID.randomUUID().toString();
        MDC.put("requestId", requestId);

        try (
                socket;
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintStream  out = new PrintStream (socket.getOutputStream(), true)
        ) {
            // Check if shutdown was requested while this thread was waiting
            if (shutdownRequested) {
                logger.debug("Server shutdown requested, closing connection");
                return;
            }
            ArrayList<String> requestLines = new ArrayList<>();
            StringBuilder fullRequest = new StringBuilder();

            Response response;

            String line;
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                requestLines.add(line);
                logger.debug(line);
                fullRequest.append(line).append("\n");
            }
            logger.debug("Parsed headers");

            Path filePath = null;

            try {
                logger.debug("Parse request");
                ClientRequest request = ClientRequest.parse(fullRequest.toString());
                logger.debug("Create path string");

                String pathStr = request.path();
                String[] splitPath = request.uri().getPath().split("/");
                boolean containsFileExtension;

                if (splitPath.length == 0) {
                    containsFileExtension = pathStr.contains(".");
                } else {
                    containsFileExtension = splitPath[splitPath.length - 1].contains(".");
                }

                boolean clientWantsHtml = request.headers().get("Accept") == null ||
                        request.headers().get("Accept").contains("text/html") ||
                        request.headers().get("Accept").contains("*/*");

                if (!containsFileExtension && clientWantsHtml) {
                    if (serverConfig.serveSingleIndex()) {
                        // In SPA mode, all HTML requests go to index.html
                        pathStr = "/index.html";
                    } else if (request.path().endsWith("/")) {
                        // In standard mode, append index.html to directory paths
                        pathStr += "index.html";
                    } else {
                        // Optional: handle non-directory paths without extensions as directories
                        pathStr += "/index.html";
                    }
                }

                logger.debug("Create path obj");
                filePath = Path.of(distFolder, pathStr);

                logger.debug("Check cache");
                if (cache.containsKey(filePath)) {
                    response = cache.get(filePath);
                } else {
                    try {
                        logger.debug("Open file");
                        StaticFile file = new StaticFile(filePath);
                        logger.debug("Get file contents");
                        byte[] body = file.getFileContents();
                        logger.debug("Create response");
                        response = new Response(200);
                        response.setHeader("Content-Type", file.getMimeType());
                        response.setHeader("Connection", "close");
                        logger.debug("Added headers");
                        response.setBody(body);
                        logger.debug("Added body");

                        cache.put(filePath, response);
                    } catch (FileNotFoundException e) {
                        response = new Response(404);
                    }
                }
            } catch (RequestParsingException e) {
                response = new Response(400);
            }

            out.writeBytes(response.asByteArray());
            out.flush();
            
            // Socket is closed automatically by try-with-resources
            
            logger.debug("Done serving {}", requestLines.getFirst());
            if (filePath == null) {
                logger.info("Sent {} response", response.getStatusCode());
            } else {
                logger.info("Sent {} response for file {}", response.getStatusCode(), filePath);
            }
        } catch (java.net.SocketTimeoutException e) {
            logger.warn("Socket timeout occurred while processing request: {}", e.getMessage());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            MDC.remove("requestId");
        }
    }
}
