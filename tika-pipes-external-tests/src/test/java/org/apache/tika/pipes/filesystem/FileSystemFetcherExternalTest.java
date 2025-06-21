package org.apache.tika.pipes.filesystem;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.FetchAndParseReply;
import org.apache.tika.FetchAndParseRequest;
import org.apache.tika.SaveFetcherReply;
import org.apache.tika.SaveFetcherRequest;
import org.apache.tika.TikaGrpc;
import org.apache.tika.pipes.fetchers.filesystem.FileSystemFetcherConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
@Slf4j
class FileSystemFetcherExternalTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    public static final int MAX_STARTUP_TIMEOUT = 120;
    static String govDocsFolder = "/tika/govdocs1"; // Volume specified in docker-compose.yml
    static File testFolder = new File("target", "govdocs1");
    static int govDocsFromIdx = 1;
    static int govDocsToIdx = 1;
    private static final DockerComposeContainer<?> composeContainer = new DockerComposeContainer<>(
            new File("src/test/resources/docker-compose.yml"))
            .withEnv("HOST_GOVDOCS1_DIR", testFolder.getAbsolutePath())
            .withStartupTimeout(Duration.of(MAX_STARTUP_TIMEOUT, ChronoUnit.SECONDS))
            .withExposedService("tika-pipes", 9090)
            .withLogConsumer("tika-pipes", new Slf4jLogConsumer(log));

    @BeforeAll
    void setup() throws Exception {
        int retries = 3;
        int attempt = 0;
        while (true) {
            try {
                downloadAndUnzipGovdocs1(govDocsFromIdx, govDocsToIdx);
                break;
            } catch (IOException e) {
                attempt++;
                if (attempt >= retries) {
                    throw e;
                }
                log.warn("Download attempt {} failed, retrying in 10 seconds...", attempt, e);
                Thread.sleep(10_000);
            }
        }
        composeContainer.start();
    }

    @AfterAll
    void close() throws IOException {
        composeContainer.close();
        //FileUtils.deleteDirectory(testFolder);
    }

    static void downloadAndUnzipGovdocs1(int fromIndex, int toIndex) throws IOException {
        String baseUrl = "https://corp.digitalcorpora.org/corpora/files/govdocs1/zipfiles";
        Path targetDir = testFolder.toPath();
        Files.createDirectories(targetDir);

        for (int i = fromIndex; i <= toIndex; i++) {
            String zipName = String.format("%03d.zip", i);
            String url = baseUrl + "/" + zipName;
            Path zipPath = targetDir.resolve(zipName);
            if (Files.exists(zipPath)) {
                log.info("{} already exists, skipping download. Delete if you would like to extract.", zipName);
                continue;
            }

            log.info("Downloading {} from {}...", zipName, url);
            try (InputStream in = new URL(url).openStream()) {
                Files.copy(in, zipPath, StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("Unzipping {}...", zipName);
            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipPath.toFile()))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    Path outPath = targetDir.resolve(entry.getName());
                    if (entry.isDirectory()) {
                        Files.createDirectories(outPath);
                    } else {
                        Files.createDirectories(outPath.getParent());
                        try (OutputStream out = Files.newOutputStream(outPath)) {
                            zis.transferTo(out);
                        }
                    }
                    zis.closeEntry();
                }
            }
        }
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
        fileSystemFetcherConfig.setPluginId("filesystem-fetcher");
        fileSystemFetcherConfig.setBasePath(govDocsFolder);
        fileSystemFetcherConfig.setExtractFileSystemMetadata(false);

        SaveFetcherReply reply = null;
        try {
            reply = blockingStub.saveFetcher(SaveFetcherRequest
                    .newBuilder()
                    .setFetcherId(fetcherId)
                    .setPluginId(fileSystemFetcherConfig.getPluginId())
                    .setFetcherConfigJson(OBJECT_MAPPER.writeValueAsString(fileSystemFetcherConfig))
                    .build());
        } catch (Exception e) {
            Assertions.fail(e);
        }
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

        try (Stream<Path> paths = Files.walk(testFolder.toPath())) {
            paths.filter(Files::isRegularFile)
                    .forEach(file -> {
                        try {
                            requestStreamObserver.onNext(FetchAndParseRequest
                                    .newBuilder()
                                    .setFetcherId(fetcherId)
                                    .setFetchKey(testFolder.toPath().relativize(file).toString())
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
