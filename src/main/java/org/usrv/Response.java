package org.usrv;

import lombok.Getter;
import lombok.Setter;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class Response {
    private String responseText;
    private static final Map<Integer, String> statuses = Map.of(
            200, "OK",
            400, "Bad Request",
            404, "Not Found",
            500, "Internal Server Error"
    );
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
            "EEE, dd MMM yyyy HH:mm:ss z",
            Locale.ENGLISH
    );

    Map<String, String> headers = new HashMap<>();

    @Getter
    @Setter
    private int StatusCode = 200;

    @Getter
    @Setter
    private String body;

    private void initializeHeaders() {
        headers.put("Server", "usrv");

        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);

        headers.put("Date", now.format(formatter));
        headers.put("Content-Type", "text/html; charset=UTF-8");
    }

    Response(int Status) {
        this.setStatusCode(Status);
        initializeHeaders();
    }

    public String toString() {
        String protocolAndStatus = String.format("HTTP/1.1 %s %s", this.getStatusCode(), statuses.get(this.getStatusCode()));
        String headersString = headers.keySet().stream().map(key -> String.format("%s: %s", key, headers.get(key))).collect(Collectors.joining("\n"));

        return String.format("%s\n%s\n\n%s", protocolAndStatus, headersString, this.getBody());
    }
}
