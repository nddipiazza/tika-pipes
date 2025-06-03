package org.apache.tika.pipes.filesystem;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.tika.FetchAndParseReply;
import org.apache.tika.FetchAndParseRequest;
import org.apache.tika.SaveFetcherReply;
import org.apache.tika.SaveFetcherRequest;
import org.apache.tika.TikaGrpc;
import org.apache.tika.pipes.fetchers.filesystem.FileSystemFetcherConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.fail;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
@Slf4j
class FileSystemFetcherExternalTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    public static final int MAX_STARTUP_TIMEOUT = 120;
    private static final DockerComposeContainer<?> composeContainer = new DockerComposeContainer<>(
            new File("src/test/resources/docker-compose.yml")).withStartupTimeout(
                    Duration.of(MAX_STARTUP_TIMEOUT, ChronoUnit.SECONDS))
            .withExposedService("tika-pipes", 9090);

    static String govDocsFolder = "/tika/govdocs1"; // Volume specified in docker-compose.yml
    static File testFolder = new File("target", "testFolder" + UUID.randomUUID());
    @BeforeAll
    void setup() {
        composeContainer.start();
        if (!testFolder.mkdirs()) {
            fail();
        }
    }

    @AfterAll
    void close() throws IOException {
        composeContainer.close();
        FileUtils.deleteDirectory(testFolder);
    }

    @Test
    void fileSystemFetcher() throws Exception {
        String fetcherId = UUID.randomUUID().toString();
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(composeContainer.getServiceHost("tika-pipes", 9090), composeContainer.getServicePort("tika-pipes", 9090))
                .usePlaintext()
                .directExecutor()
                .build();
        TikaGrpc.TikaBlockingStub blockingStub = TikaGrpc.newBlockingStub(channel);
        TikaGrpc.TikaStub tikaStub = TikaGrpc.newStub(channel);

        FileSystemFetcherConfig fileSystemFetcherConfig = new FileSystemFetcherConfig();
        fileSystemFetcherConfig.setPluginId("file-system-fetcher");
        fileSystemFetcherConfig.setBasePath(govDocsFolder);
        fileSystemFetcherConfig.setExtractFileSystemMetadata(false);

        SaveFetcherReply reply = blockingStub.saveFetcher(SaveFetcherRequest
                .newBuilder()
                .setFetcherId(fetcherId)
                .setPluginId(fileSystemFetcherConfig.getPluginId())
                .setFetcherConfigJson(OBJECT_MAPPER.writeValueAsString(fileSystemFetcherConfig))
                .build());
        log.info("Saved fetcher with ID {}", reply.getFetcherId());

        List<FetchAndParseReply> successes = Collections.synchronizedList(new ArrayList<>());
        List<FetchAndParseReply> errors = Collections.synchronizedList(new ArrayList<>());

        CountDownLatch countDownLatch = new CountDownLatch(1);
        StreamObserver<FetchAndParseRequest> requestStreamObserver = tikaStub.fetchAndParseBiDirectionalStreaming(new StreamObserver<>() {
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

        try (Stream<Path> paths = Files.walk(Paths.get(govDocsFolder))) {
            paths.filter(Files::isRegularFile)
                    .forEach(file -> {
                        try {
                            requestStreamObserver.onNext(FetchAndParseRequest
                                    .newBuilder()
                                    .setFetcherId(fetcherId)
                                    .setFetchKey(file.toFile().getAbsolutePath())
                                    .setFetchMetadataJson(OBJECT_MAPPER.writeValueAsString(Map.of()))
                                    .build());
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
        log.info("Done submitting URLs to {}", fetcherId);

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
