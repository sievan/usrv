package org.usrv.http;

import org.usrv.exceptions.InvalidRequestException;
import org.usrv.exceptions.RequestParsingException;

import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public record ClientRequest(String method, String path, String protocol, Map<String, String> headers, URI uri) {
    static Set<String> supportedMethods = Set.of("GET");

    public static ClientRequest parse(String request) {
        try {
            String[] requestLines = request.split("\n");

            String requestLine = requestLines[0];

            String method = requestLine.split(" ")[0];
            String uriString = requestLine.split(" ")[1];
            String protocol = requestLine.split(" ")[2];

            String[] headerLines = Arrays.copyOfRange(requestLines, 1, requestLines.length);

            Map<String, String> headers = Arrays.stream(headerLines)
                    .map(line -> line.split(": "))
                    .collect(
                            Collectors.toMap(parts -> parts[0], parts -> parts[1])
                    );

            URI uri = URI.create(uriString);

            return new ClientRequest(method, uri.getPath(), protocol, headers, uri);
        } catch (Exception e) {
            throw new RequestParsingException("Failed to parse request: ", e);
        }
    }

    public void validate() {
        if (!supportedMethods.contains(this.method)) {
            throw new InvalidRequestException("Unsupported method: " + this.method);
        } else if (!Objects.equals(this.protocol, "HTTP/1.1")) {
            throw new InvalidRequestException("Unsupported protocol: " + this.protocol);
        }
    }
}
