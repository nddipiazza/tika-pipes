package org.apache.tika.pipes.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.undertow.Undertow;
import io.undertow.util.Headers;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.TikaGrpc;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
@Slf4j
class HttpFetcherExternalTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Logger LOG = LoggerFactory.getLogger(HttpFetcherExternalTest.class);
    public static final int MAX_STARTUP_TIMEOUT = 120;
    private static final DockerComposeContainer<?> composeContainer = new DockerComposeContainer<>(
            new File("src/test/resources/docker-compose.yml")).withStartupTimeout(
                    Duration.of(MAX_STARTUP_TIMEOUT, ChronoUnit.SECONDS))
            .withExposedService("tika-pipes", 50051);
    public static int getRandomAvailablePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException("Could not find an available port", e);
        }
    }
    static int httpServerPort;
    static Undertow server;
    @BeforeAll
    void setup() throws Exception {
        httpServerPort = getRandomAvailablePort();
        server = Undertow
                .builder()
                .addHttpListener(httpServerPort, InetAddress.getLocalHost().getHostAddress())
                .setHandler(exchange -> {
                    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html");
                    exchange.getResponseSender().send("<html><body>test</body></html>");
                }).build();

        composeContainer.start();
    }

    @AfterAll
    void close() {
        composeContainer.close();
        server.stop();
    }

    @Test
    void httpFetcher() throws Exception {
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(host, port)
                .usePlaintext()
                .directExecutor()
                .build();
        TikaGrpc.TikaBlockingStub blockingStub = TikaGrpc.newBlockingStub(channel);
        TikaGrpc.TikaStub tikaStub = TikaGrpc.newStub(channel);

    }
}
