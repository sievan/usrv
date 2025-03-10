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

import java.io.*;
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
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
    ) {
        boolean keepAlive = true;

        while (keepAlive) {
            Response response;
            Path filePath = null;
            ClientRequest request;
            PathResolver pathResolver = new PathResolver(serverConfig);

            try {
                if (in.ready()) {
                    logger.debug("Parse request");
                    request = ClientRequest.parseBuffer(in);

                    logger.debug("Validate request");
                    request.validate();

                    keepAlive = request.isKeepAlive();

                    logger.debug("Resolve file path");
                    filePath = pathResolver.resolveRequest(request);

                    boolean isHeadMethod = request.method().equals("HEAD");

                    logger.debug("Check cache");
                    if (cache.containsKey(filePath)) {
                        response = cache.get(filePath);
                        response.setHeader("Connection", keepAlive ? "keep-alive" : "close");
                    } else {
                        try {
                            logger.debug("Open file");
                            StaticFile file = new StaticFile(filePath);
                            logger.debug("Get file contents");
                            String body = file.getFileContents();
                            logger.debug("Create response");
                            response = new Response(200);
                            response.setHeader("Content-Type", file.getMimeType());
                            response.setHeader("Connection", keepAlive ? "keep-alive" : "close");

                            if (!isHeadMethod) {
                                response.setBody(body);
                                cache.put(filePath, response);
                            }
                            logger.debug("Response headers: {}", response.getHeaders());
                        } catch (FileNotFoundException e) {
                            response = new Response(404);
                        }
                    }
                } else {
                    continue;
                }
            } catch (RequestParsingException | InvalidRequestException e) {
                logger.warn("Error processing request: {}", e.getMessage());
                response = new Response(400);
                keepAlive = false;
            } catch (java.net.SocketTimeoutException e) {
                logger.warn("Socket timeout occurred, closing connection.");
                break;
            }

            out.println(response);
            out.flush();

            if (filePath == null) {
                logger.info("Sent {} response", response.getStatusCode());
            } else {
                logger.info("Sent {} response for file {}", response.getStatusCode(), filePath);
            }

            if (!keepAlive) {
                logger.debug("Closing connection as per request.");
                break;
            }
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
