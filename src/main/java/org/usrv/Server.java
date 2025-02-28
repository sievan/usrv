package org.usrv;

import lombok.Setter;
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
    private boolean shouldRun = true;

    private final String distFolder;

    private final ServerConfig serverConfig;

    private final Map<Path, Response> cache = new ConcurrentHashMap<>();

    Server() {
        this(ServerConfig.getDefaultConfig());
    }

    Server(ServerConfig config) {
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

    public void stop() {
        shouldRun = false;
    }

    private void handleRequest(Socket socket) {
        Logger logger = new Logger();

        try (
                socket;
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        ) {
            ArrayList<String> requestLines = new ArrayList<>();
            StringBuilder fullRequest = new StringBuilder();

            Response response;

            String line;
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                requestLines.add(line);
                logger.log(line);
                fullRequest.append(line).append("\n");
            }
            logger.log("Parsed headers");

            Path filePath = null;

            try {
                logger.log("Parse request");
                ClientRequest request = ClientRequest.parse(fullRequest.toString());
                logger.log("Create path string");

                String pathStr = request.path();
                String[] splitPath = request.uri().getPath().split("/");
                boolean containsFileExtension;

                if (splitPath.length == 0) {
                    containsFileExtension = pathStr.contains(".");
                } else {
                    containsFileExtension = splitPath[splitPath.length - 1].contains(".");
                }

                boolean clientWantsHtml = request.headers().get("Accept") == null || request.headers().get("Accept").contains("text/html");

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

                logger.log("Create path obj");
                filePath = Path.of(distFolder, pathStr);

                logger.log("Check cache");
                if (cache.containsKey(filePath)) {
                    response = cache.get(filePath);
                } else {
                    try {
                        logger.log("Open file");
                        StaticFile file = new StaticFile(filePath);
                        logger.log("Get file contents");
                        String body = file.getFileContents();
                        logger.log("Create response");
                        response = new Response(200);
                        response.setHeader("Content-Type", file.getMimeType());
                        response.setBody(body);
                        response.setHeader("Connection", "close");
                        logger.log("Added headers");
                        cache.put(filePath, response);
                    } catch (FileNotFoundException e) {
                        response = new Response(404);
                    }
                }
            } catch (RequestParsingException e) {
                response = new Response(400);
            }

            out.println(response);
            out.flush();

            socket.close();

            logger.log("Done serving " + requestLines.getFirst());
            if (filePath == null) {
                logger.logf("Sent %d response\n", response.getStatusCode());
            } else {
                logger.logf("Sent %d response for file %s\n", response.getStatusCode(), filePath);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
