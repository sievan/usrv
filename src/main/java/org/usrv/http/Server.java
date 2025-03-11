package org.usrv.http;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;

import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.usrv.config.ServerConfig;
import org.usrv.exceptions.InvalidRequestException;
import org.usrv.file.PathResolver;
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

    private final ServerConfig serverConfig;

    private final Map<Path, Response> cache = new ConcurrentHashMap<>();

    private final static Logger logger = LoggerFactory.getLogger(Server.class);

    public Server() {
        this(ServerConfig.getDefaultConfig());
    }

    public Server(ServerConfig config) {
        this.serverConfig = config;
        this.port = config.port();
    }

    public void start() {
        try (ServerSocket socket = new ServerSocket(port, 1000)) {
            System.out.printf("Server started at port: %s%n", port);

            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                while (shouldRun) {
                    Socket clientSocket = socket.accept();
                    // Check if shutdown was requested while this thread was waiting
                    if (!shouldRun) {
                        logger.debug("Server shutdown requested, closing connection");
                        return;
                    }
                    clientSocket.setSoTimeout(30000);
                    executor.submit(() -> {
                        try {
                            handleRequest(clientSocket);
                        } catch (Throwable t) {
                            logger.error("Fatal error in request handler: {}", t.getMessage(), t);
                        }
                    });
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        shouldRun = false;
    }

    private void handleRequest(Socket socket) {
        String requestId = UUID.randomUUID().toString();
        MDC.put("requestId", requestId);

        try (
                socket;
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintStream out = new PrintStream(socket.getOutputStream(), true)
        ) {
            Response response;
            Path filePath = null;
            ClientRequest request;
            PathResolver pathResolver = new PathResolver(serverConfig);

            try {
                logger.debug("Parse request");
                request = ClientRequest.parseBuffer(in);

                logger.debug("Validate request");
                request.validate();

                logger.debug("Resolve file path");
                filePath = pathResolver.resolveRequest(request);

                boolean isHeadMethod = request.method().equals("HEAD");

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
                        response.setHeader("Content-Length", String.valueOf(body.length));
                        response.setHeader("Connection", "close");
                        logger.debug("Added headers");

                        if (!isHeadMethod) {
                            response.setBody(body);
                            logger.debug("Added body");
                            cache.put(filePath, response);
                        }
                    } catch (FileNotFoundException e) {
                        response = new Response(404);
                    }
                }
            } catch (RequestParsingException | InvalidRequestException e) {
                response = new Response(400);
            }
            if (!response.headers.containsKey("Content-Length")) {
                response.setHeader("Content-Length", "0");
            }
            out.print(response.getFullResponseHeaders());
            out.writeBytes(response.getBody());
            out.flush();

            if (filePath == null) {
                logger.info("Sent {} response", response.getStatusCode());
            } else {
                logger.info("Sent {} response for file {}", response.getStatusCode(), filePath);
            }
        } catch (java.net.SocketTimeoutException e) {
            logger.warn("Socket timeout occurred while processing request: {}", e.getMessage());
        } catch (IOException e) {
            logger.error("I/O error handling request: {}", e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Uncaught exception in request handler: {}", e.getMessage(), e);
        } finally {
            MDC.remove("requestId");
        }
    }
}
