package org.usrv;

import lombok.Setter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Server {
    public static final int port = 80;

    @Setter
    private boolean shouldRun = true;

    private final String distFolder;

    private final Map<Path, Response> cache = new HashMap<>();

    Server() {
        this("./dist");
    }

    Server(String distFolder) {
        this.distFolder = distFolder;
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

    private void handleRequest(Socket socket) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            ArrayList<String> requestLines = new ArrayList<>();
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            Response response;

            String line;
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                requestLines.add(line);
            }

            Path filePath = Path.of(distFolder, "index.html");

            if (cache.containsKey(filePath)) {
                response = cache.get(filePath);
            } else {
                StaticFile file = new StaticFile(filePath);
                String body = file.getFileContents();
                response = new Response();
                response.setStatusCode(200);
                response.setBody(body);
                cache.put(filePath, response);
            }

            out.println(response);
            out.flush();


            socket.close();

            System.out.print("Served request: ");
            System.out.println(requestLines.getFirst());
            System.out.printf("Sent %d response for file %s\n", response.getStatusCode(), filePath);
            System.out.println();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
