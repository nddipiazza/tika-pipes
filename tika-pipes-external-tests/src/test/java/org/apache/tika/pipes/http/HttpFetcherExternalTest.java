package org.apache.tika.pipes.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import io.undertow.Undertow;
import io.undertow.util.Headers;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.FetchAndParseReply;
import org.apache.tika.FetchAndParseRequest;
import org.apache.tika.SaveFetcherReply;
import org.apache.tika.SaveFetcherRequest;
import org.apache.tika.TikaGrpc;
import org.apache.tika.pipes.fetchers.http.config.HttpFetcherConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
@Slf4j
class HttpFetcherExternalTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    public static final int MAX_STARTUP_TIMEOUT = 120;
    private static final DockerComposeContainer<?> composeContainer = new DockerComposeContainer<>(
            new File("src/test/resources/docker-compose.yml")).withStartupTimeout(
                    Duration.of(MAX_STARTUP_TIMEOUT, ChronoUnit.SECONDS))
            .withExposedService("tika-pipes", 9090);
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
        String fetcherId = UUID.randomUUID().toString();
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(composeContainer.getServiceHost("tika-pipes", 9090), composeContainer.getServicePort("tika-pipes", 9090))
                .usePlaintext()
                .directExecutor()
                .build();
        TikaGrpc.TikaBlockingStub blockingStub = TikaGrpc.newBlockingStub(channel);
        TikaGrpc.TikaStub tikaStub = TikaGrpc.newStub(channel);

        HttpFetcherConfig httpFetcherConfig = new HttpFetcherConfig();
        httpFetcherConfig.setPluginId("http-fetcher");

        SaveFetcherReply reply = blockingStub.saveFetcher(SaveFetcherRequest
                .newBuilder()
                .setFetcherId(fetcherId)
                .setPluginId(httpFetcherConfig.getPluginId())
                .setFetcherConfigJson(OBJECT_MAPPER.writeValueAsString(httpFetcherConfig))
                .build());
        log.info("Saved fetcher with ID {}", reply.getFetcherId());

        List<FetchAndParseReply> successes = Collections.synchronizedList(new ArrayList<>());
        List<FetchAndParseReply> errors = Collections.synchronizedList(new ArrayList<>());

        CountDownLatch countDownLatch = new CountDownLatch(1);
        StreamObserver<FetchAndParseRequest> requestStreamObserver = tikaStub.fetchAndParseBiDirectionalStreaming(new StreamObserver<FetchAndParseReply>() {
            @Override
            public void onNext(FetchAndParseReply fetchAndParseReply) {
                log.debug("Reply from fetch-and-parse - key={}, metadata={}", fetchAndParseReply.getFetchKey(), fetchAndParseReply.getMetadataList());
                if ("FetchException"
                        .equals(fetchAndParseReply.getStatus())) {
                    errors.add(fetchAndParseReply);
                } else {
                    successes.add(fetchAndParseReply);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                log.error("Received an error", throwable);
                countDownLatch.countDown();
            }

            @Override
            public void onCompleted() {
                log.info("Completed fetch and parse");
                countDownLatch.countDown();
            }
        });

        log.info("Done submitting URLs to {}", fetcherId);

        requestStreamObserver.onNext(FetchAndParseRequest
                .newBuilder()
                .setFetcherId(fetcherId)
                .setFetchKey("http://localhost:" + httpServerPort)
                .setFetchMetadataJson(OBJECT_MAPPER.writeValueAsString(Map.of()))
                .build());

        requestStreamObserver.onCompleted();

        try {
            if (!countDownLatch.await(3, TimeUnit.MINUTES)) {
                log.error("Timed out waiting for parse to complete");
            }
        } catch (InterruptedException e) {
            Thread
                    .currentThread()
                    .interrupt();
        }
        log.info("Fetched: success={}", successes);
    }
}
