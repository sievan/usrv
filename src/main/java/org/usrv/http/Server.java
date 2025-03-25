package org.usrv.http;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;

import org.slf4j.LoggerFactory;
import org.usrv.config.ServerConfig;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;

public class Server {
    public final int port;

    @Setter
    @Getter
    private boolean shouldRun = true;

    private final ServerConfig serverConfig;

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
                            RequestHandler handler = new RequestHandler(serverConfig);
                            handler.handleRequest(clientSocket);
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


}
