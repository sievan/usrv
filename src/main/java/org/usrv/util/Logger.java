package org.usrv.util;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Logger {
    private final String requestId;
    private static final ExecutorService logExecutor = Executors.newSingleThreadExecutor();

    public Logger() {
        requestId = UUID.randomUUID().toString();
    }

    public void log(String message) {
        logExecutor.submit(() -> {
            System.out.printf("%s | %s", requestId, message);
            System.out.println();
        });
    }

    public void logf(String message, Object... args) {
        logExecutor.submit(() -> {
            String formattedMessage = String.format(message, args);
            System.out.printf("%s | %s", requestId, formattedMessage);
            System.out.println();
        });
    }

    public void log() {
        logExecutor.submit(() -> {
            System.out.printf("%s |", requestId);
            System.out.println();
        });
    }

    // Important: Call this method when your application is shutting down
    public static void shutdown() {
        logExecutor.shutdown();
    }

    // Factory method to create new logger instances
    public static Logger create() {
        return new Logger();
    }
}