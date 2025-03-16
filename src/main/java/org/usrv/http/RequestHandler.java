package org.usrv.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.usrv.config.ServerConfig;
import org.usrv.exceptions.InvalidRequestException;
import org.usrv.exceptions.RequestParsingException;
import org.usrv.file.PathResolver;
import org.usrv.file.StaticFile;

import java.io.*;
import java.net.Socket;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(RequestHandler.class);

    private static final Map<Path, Response> cache = new ConcurrentHashMap<>();

    private final ServerConfig serverConfig;

    public RequestHandler(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    public void handleRequest(Socket socket) {
        String requestId = UUID.randomUUID().toString();
        MDC.put("requestId", requestId);

        try {
            processRequest(socket);
        } catch (java.net.SocketTimeoutException e) {
            logger.warn("Socket timeout occurred while processing request: {}", e.getMessage());
        } catch (IOException e) {
            logger.error("I/O error handling request: {}", e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Uncaught exception in request handler: {}", e.getMessage(), e);
            logger.error("Exception type: {}", e.getClass().getName());
        } finally {
            MDC.remove("requestId");
        }
    }

    private void processRequest(Socket socket) throws IOException {
        Response response;
        Path filePath = null;
        ClientRequest request;
        PathResolver pathResolver = new PathResolver(serverConfig);

        boolean keepAlive = true;

        try (
                socket;
                PrintStream out = new PrintStream(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            while (keepAlive) {
                try {
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
                    } else {
                        try {
                            logger.debug("Cache miss. Generating a response.");
                            response = generateFileResponse(filePath, isHeadMethod);
                        } catch (FileNotFoundException e) {
                            response = new Response(404);
                        }
                    }
                } catch (RequestParsingException | InvalidRequestException e) {
                    logger.warn("Error processing request: {}", e.getMessage());
                    response = new Response(400);
                    keepAlive = false;
                } catch (java.net.SocketTimeoutException e) {
                    logger.warn("Socket timeout occurred, closing connection.");
                    break;
                }

                response.setHeader("Connection", keepAlive ? "keep-alive" : "close");

                sendResponse(out, response);

                if (filePath == null) {
                    logger.info("Sent {} response", response.getStatusCode());
                } else {
                    logger.info("Sent {} response for file {}", response.getStatusCode(), filePath);
                }

                if (!keepAlive) {
                    logger.debug("Closing connection");
                    break;
                }
            }
        }
    }

    private Response generateFileResponse(Path filePath, boolean isHeadMethod) throws IOException {
        logger.debug("Open file");
        StaticFile file = new StaticFile(filePath);
        logger.debug("Get file contents");

        byte[] body = file.getFileContents();

        logger.debug("Create response");
        Response response = new Response(200);
        response.setHeader("Content-Type", file.getMimeType());
        response.setHeader("Content-Length", String.valueOf(body.length));


        if (!isHeadMethod) {
            response.setBody(body);
            logger.debug("Added body");
            cache.put(filePath, response);
        }

        return response;
    }

    private void sendResponse(PrintStream out, Response response) {
        out.print(response.getFullResponseHeaders());

        if (response.getBody() != null && response.getBody().length > 0) {
            out.writeBytes(response.getBody());
        } else {
            out.writeBytes(new byte[0]);
        }

        out.flush();
    }
}
