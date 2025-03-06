package org.usrv.http;

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

    @Getter
    Map<String, String> headers = new HashMap<>();

    @Getter
    @Setter
    private int StatusCode = 200;

    @Getter
    @Setter
    private byte[] body;

    private void initializeHeaders() {
        headers.put("Server", "usrv");

        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);

        headers.put("Date", now.format(formatter));
    }

    public Response(int Status) {
        this.setStatusCode(Status);
        initializeHeaders();
    }

    public String toString() {
        String protocolAndStatus = String.format("HTTP/1.1 %s %s", this.getStatusCode(), statuses.get(this.getStatusCode()));
        String headersString = headers.keySet().stream().map(key -> String.format("%s: %s", key, headers.get(key))).collect(Collectors.joining("\n"));
        String bodyString = body == null ? "" : new String(body);

        return String.format("%s\n%s\n\n%s", protocolAndStatus, headersString, bodyString);
    }

    public byte[] toByteArray() {
        String protocolAndStatus = String.format("HTTP/1.1 %s %s", this.getStatusCode(), statuses.get(this.getStatusCode()));
        String headersString = headers.keySet().stream().map(key -> String.format("%s: %s", key, headers.get(key))).collect(Collectors.joining("\n"));
        byte[] headersByteArray = String.format("%s\n%s\n\n", protocolAndStatus, headersString).getBytes();
        byte[] responseByteArray = new byte[headersByteArray.length + this.getBody().length];
        System.arraycopy(headersByteArray, 0, responseByteArray, 0, headersByteArray.length);
        System.arraycopy(this.getBody(), 0, responseByteArray, headersByteArray.length, this.getBody().length);
        return responseByteArray;
    }

    public void setHeader(String headerName, String value) {
        this.headers.put(headerName, value);
    }
}
