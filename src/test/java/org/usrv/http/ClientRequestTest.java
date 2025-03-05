package org.usrv.http;

import org.junit.jupiter.api.*;
import org.usrv.exceptions.RequestParsingException;

import java.io.*;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClientRequestTest {

    ClientRequest sendAndParseRequest(String requestString) {
        BufferedReader reader = new BufferedReader(new StringReader(requestString));

        return ClientRequest.parseBuffer(reader);
    }

    @Test
    @DisplayName("A simple request with headers should be successfully parsed")
    void testParseHeaders() {
        String requestString = """
                GET / HTTP/1.1
                Host: localhost
                User-Agent: insomnia/10.3.1
                Accept: */*
                """;

        BufferedReader reader = new BufferedReader(new StringReader(requestString));

        ClientRequest.parseBuffer(reader);
    }

    @Test
    @DisplayName("After parsing a request, you should be able to check the method, path and protocol")
    void testParseAndReturnMethod() {
        String requestString = """
                GET / HTTP/1.1
                Host: localhost
                User-Agent: insomnia/10.3.1
                Accept: */*
                """;

        BufferedReader reader = new BufferedReader(new StringReader(requestString));

        ClientRequest request = ClientRequest.parseBuffer(reader);

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

        BufferedReader reader = new BufferedReader(new StringReader(requestString));

        ClientRequest request = ClientRequest.parseBuffer(reader);

        assertEquals(headerLines[0].split(" ")[1], request.headers().get("Host"));
        assertEquals(headerLines[1].split(" ")[1], request.headers().get("User-Agent"));
        assertEquals(headerLines[2].split(" ")[1], request.headers().get("Accept"));
    }


    @Test
    @DisplayName("It should throw an error upon parsing a malformed request")
    void testExceptions() {
        String requestString = """
                GET HTTP/1.1
                Host: localhost
                User-Agent: insomnia/10.3.1
                Accept: */*
                """;

        BufferedReader reader = new BufferedReader(new StringReader(requestString));

        Exception exception = assertThrows(RequestParsingException.class, () -> ClientRequest.parseBuffer(reader));

        assertEquals("Failed to parse request line: ", exception.getMessage());
    }


    @Test
    @DisplayName("For get requests, the full body should be consumed addition to headers")
    void testRequestBody() throws Exception {
        String requestLine = "GET / HTTP/1.1\n";
        String[] headerLines = {
                "Host: localhost",
                "User-Agent: insomnia/10.3.1",
                "Accept: */*",
                "Content-Type: application/json",
                "Content-Length: 39"
        };
        String[] bodyLines = {
                "\n",
                "{\n",
                "  \"key1\": \"value1\",\n",
                "  \"key2\": \"value2\"\n",
                "}"
        };
        String requestString = requestLine +
                String.join("\n", headerLines) +
                String.join("\n", bodyLines);

        BufferedReader reader = new BufferedReader(new StringReader(requestString));

        ClientRequest request = ClientRequest.parseBuffer(reader);

        assertNull(reader.readLine());

        assertEquals(headerLines[0].split(" ")[1], request.headers().get("Host"));
        assertEquals(headerLines[1].split(" ")[1], request.headers().get("User-Agent"));
        assertEquals(headerLines[2].split(" ")[1], request.headers().get("Accept"));
        assertEquals(headerLines[3].split(" ")[1], request.headers().get("Content-Type"));
        assertEquals(headerLines[4].split(" ")[1], request.headers().get("Content-Length"));
    }

    @Test
    @DisplayName("For head requests, only headers should be consumed, no body")
    void testHeadRequest() throws Exception {
        String requestLine = "GET / HTTP/1.1\n";
        String[] headerLines = {
                "Host: localhost",
                "User-Agent: insomnia/10.3.1",
                "Accept: */*",
                "Content-Type: application/json",
                "Content-Length: 39"
                
        };

        String requestString = requestLine +
                String.join("\n", headerLines);

        BufferedReader reader = new BufferedReader(new StringReader(requestString));

        ClientRequest request = ClientRequest.parseBuffer(reader);

        assertNull(reader.readLine());

        assertEquals(headerLines[0].split(" ")[1], request.headers().get("Host"));
        assertEquals(headerLines[1].split(" ")[1], request.headers().get("User-Agent"));
        assertEquals(headerLines[2].split(" ")[1], request.headers().get("Accept"));
        assertEquals(headerLines[3].split(" ")[1], request.headers().get("Content-Type"));
        assertEquals(headerLines[4].split(" ")[1], request.headers().get("Content-Length"));
    }
}