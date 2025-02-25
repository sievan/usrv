package org.usrv;

import lombok.Setter;
import org.usrv.exceptions.RequestParsingException;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.*;

public class Server {
    public final int port;

    @Setter
    private boolean shouldRun = true;

    private final String distFolder;

    private final Map<Path, Response> cache = new HashMap<>();

    Server() {
        this(ServerConfig.getDefaultConfig());
    }

    Server(ServerConfig config) {
        this.distFolder = config.distFolder();
        this.port = config.port();
    }

    public void start() {
        try (ServerSocket socket = new ServerSocket(port)) {
            System.out.printf("Server started at port: %s%n", port);

            while (shouldRun) {
                Socket clientSocket = socket.accept();
                handleRequest(clientSocket);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        shouldRun = false;
    }

    private void handleRequest(Socket socket) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            ArrayList<String> requestLines = new ArrayList<>();
            StringBuilder fullRequest = new StringBuilder();
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            Response response;

            String line;
            System.out.println();
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                requestLines.add(line);
                System.out.println(line);
                fullRequest.append(line).append("\n");
            }
            System.out.println();

            Path filePath = null;

            try {
                ClientRequest request = ClientRequest.parse(fullRequest.toString());
                String pathStr = request.path() + (request.path().endsWith("/") ? "index.html" : "");
                filePath = Path.of(distFolder, pathStr);

                if (cache.containsKey(filePath)) {
                    response = cache.get(filePath);
                } else {
                    try {
                        StaticFile file = new StaticFile(filePath);
                        String body = file.getFileContents();
                        response = new Response(200);
                        response.setHeader("Content-Type", file.getMimeType());
                        response.setBody(body);
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

            System.out.print("Served request: ");
            System.out.println(requestLines.getFirst());
            if (filePath == null) {
                System.out.printf("Sent %d response\n", response.getStatusCode());
            } else {
                System.out.printf("Sent %d response for file %s\n", response.getStatusCode(), filePath);
            }
            System.out.println();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
