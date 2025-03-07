package org.usrv.http;

import org.usrv.exceptions.InvalidRequestException;
import org.usrv.exceptions.RequestParsingException;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

public record ClientRequest(String method, String path, String protocol, Map<String, String> headers, URI uri) {
    static Set<String> supportedMethods = Set.of("GET");

    private record HttpRequestLine(String method, String uriString, String protocol) {
    }

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

    private static HttpRequestLine parseRequestLine(String line) {
        try {
            String[] splitLine = line.split(" ");

            return new HttpRequestLine(splitLine[0], splitLine[1], splitLine[2]);
        } catch (Exception e) {
            throw new RequestParsingException("Failed to parse request line: ", e);
        }
    }

    public static ClientRequest parseBuffer(BufferedReader reader) {
        try {
            String requestLine = reader.readLine();

            HttpRequestLine httpRequestLine = parseRequestLine(requestLine);

            Map<String, String> headers = new HashMap<>();

            String headerLine;
            while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
                String[] parts = headerLine.split(": ");
                headers.put(parts[0], parts[1]);
            }

            while (reader.ready() && reader.readLine() != null) {
                // Read and discard all additional data after headers
                // since GET and HEAD requests may contain a body, but it's always irrelevant
            }

            URI uri = URI.create("/");

            return new ClientRequest(
                    httpRequestLine.method(), httpRequestLine.uriString(), httpRequestLine.protocol(), headers, uri
            );
        } catch (IOException | ArrayIndexOutOfBoundsException e) {
            throw new RequestParsingException(e.getMessage());
        }
    }

    public void validate() {
        if (!supportedMethods.contains(this.method)) {
            throw new InvalidRequestException("Unsupported method: " + this.method);
        } else if (!Objects.equals(this.protocol, "HTTP/1.1")) {
            throw new InvalidRequestException("Unsupported protocol: " + this.protocol);
        } else if(this.isHostRequiredForProtocol() && !this.headers.containsKey("Host")) {
            throw new InvalidRequestException(InvalidRequestException.MISSING_REQUIRED_HOST_MESSAGE);
        }
    }

    private boolean isHostRequiredForProtocol(){
        Set<String> protocolsWithRequiredHost = Set.of("HTTP/1.1", "HTTP/2", "HTTP/3");

        return protocolsWithRequiredHost.contains(this.protocol);
    }
}
