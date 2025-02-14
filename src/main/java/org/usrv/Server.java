package org.usrv;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server {
    public final int port = 80;
    public ServerSocket socket;

    Server() {
        try {
            socket = new ServerSocket(port);
            System.out.printf("Server started at port: %s%n", String.valueOf(port));

            while (true) {
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

            while (in.ready()) {
                String line = in.readLine();
                System.out.println(line);
                requestLines.add(line);
            }

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            Response response = new Response();
            response.setStatusCode(200);
            response.setBody("<html>Hej!</html>");

            out.println(response);

            System.out.printf("Sent %s response for %s", response.getStatusCode(), requestLines.getFirst());

            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
