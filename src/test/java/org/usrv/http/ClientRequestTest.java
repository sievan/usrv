package org.usrv.http;

import org.junit.jupiter.api.*;
import org.usrv.exceptions.RequestParsingException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClientRequestTest {

    @Test
    @DisplayName("A simple request with headers should be successfully parsed")
    void testParseHeaders() {
        String requestString = "GET / HTTP/1.1"
                + "Host: localhost"
                + "User-Agent: insomnia/10.3.1"
                + "Accept: */*";

        ClientRequest request = ClientRequest.parse(requestString);
    }

    @Test
    @DisplayName("After parsing a request, you should be able to check the method, path and protocol")
    void testParseAndReturnMethod() {
        String requestString = "GET / HTTP/1.1\n"
                + "Host: localhost\n"
                + "User-Agent: insomnia/10.3.1\n"
                + "Accept: */*\n";

        ClientRequest request = ClientRequest.parse(requestString);

        assertEquals("GET", request.method());
        assertEquals("/", request.path());
        assertEquals("HTTP/1.1", request.protocol());
    }

    @Test
    @DisplayName("After parsing a request, you should be able to check the headers independently")
    void testGetHeaders() {
        String requestLine = "GET / HTTP/1.1\n";
        String[] headerLines = {
                "Host: localhost",
                "User-Agent: insomnia/10.3.1",
                "Accept: */*"
        };
        String requestString = requestLine +
                String.join("\n", headerLines);

        ClientRequest request = ClientRequest.parse(requestString);

        Map<String, String> headers = request.headers();


        assertEquals(headerLines[0].split(" ")[1], request.headers().get("Host"));
        assertEquals(headerLines[1].split(" ")[1], request.headers().get("User-Agent"));
        assertEquals(headerLines[2].split(" ")[1], request.headers().get("Accept"));
    }


    @Test
    @DisplayName("It should throw an error upon parsing a malformed request")
    void testExceptions() {
        String requestString = "GET HTTP/1.1\n"
                + "Host: localhost\n"
                + "User-Agent: insomnia/10.3.1\n"
                + "Accept: */*\n";

        Exception exception = assertThrows(RequestParsingException.class, () -> ClientRequest.parse(requestString));

        assertEquals("Failed to parse request: ", exception.getMessage());
    }
}