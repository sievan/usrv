package org.usrv;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ResponseTests {


    @Test
    @DisplayName("A 200 response can be created with default values inferred")
    void testBasicHttpResponse() {
        Response response = new Response(200);
        response.setBody("<html>Hello</html>");

        String responseText = response.toString();

        assertTrue(responseText.contains("HTTP/1.1 200 OK"));
        assertTrue(responseText.contains("Content-Type: text/html"));
        assertTrue(responseText.contains("<html>Hello</html>"));
    }

    @Test
    @DisplayName("Responses for all relevant status codes can be created with default values inferred")
    void testStatuses() {
        Response ok = new Response(200);
        assertThat(ok.toString(), containsString("HTTP/1.1 200 OK"));

        Response badRequest = new Response(400);
        assertThat(badRequest.toString(), containsString("HTTP/1.1 400 Bad Request"));

        Response notFound = new Response(404);
        assertThat(notFound.toString(), containsString("HTTP/1.1 404 Not Found"));

        Response serverError = new Response(500);
        assertThat(serverError.toString(), containsString("HTTP/1.1 500 Internal Server Error"));
    }
}