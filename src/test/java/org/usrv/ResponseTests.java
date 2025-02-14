package org.usrv;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResponseTests {


    @Test
    void testBasicHttpResponse() {
        Response response = new Response();
        response.setStatusCode(200);
        response.setBody("<html>Hello</html>");

        String responseText = response.toString();

        assertTrue(responseText.contains("HTTP/1.1 200 OK"));
        assertTrue(responseText.contains("Content-Type: text/html"));
        assertTrue(responseText.contains("<html>Hello</html>"));
    }
//
//    @Test
//    void test404Response() {
//        Response response = new Response();
//        response.setStatusCode(404);
//
//        String responseText = response.toString();
//
//        assertTrue(responseText.contains("HTTP/1.1 404 Not Found"));
//    }
}