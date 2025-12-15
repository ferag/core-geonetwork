package org.fao.geonet.handle.client;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertTrue;

public class HandleRestClientTest {
    private HttpServer server;
    private String lastBody;
    private int port;

    @Before
    public void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();
        server.createContext("/api/handles/", new RecordingHandler());
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
    }

    @After
    public void tearDown() {
        server.stop(0);
    }

    @Test
    public void testCreateOrUpdate() throws Exception {
        HandleRestClient client = new HandleRestClient(
            "http://localhost:" + port + "/api/handles",
            "user",
            "pass",
            "adminData");

        client.createOrUpdate("prefix/123", "http://example.com", true);

        assertTrue(lastBody.contains("\"http://example.com\""));
        assertTrue(lastBody.contains("HS_ADMIN"));
    }

    private class RecordingHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            lastBody = readBody(exchange.getRequestBody());
            byte[] response = "ok".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        }

        private String readBody(InputStream inputStream) throws IOException {
            byte[] buffer = new byte[4096];
            int read = inputStream.read(buffer);
            return new String(buffer, 0, read, StandardCharsets.UTF_8);
        }
    }
}
