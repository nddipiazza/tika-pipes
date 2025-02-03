package org.apache.tika.pipes.s3;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.*;
import org.apache.tika.pipes.fetchers.s3.config.S3FetcherConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.net.URI;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
@Slf4j
class S3FetcherExternalTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Logger LOG = LoggerFactory.getLogger(S3FetcherExternalTest.class);

    public static final int MAX_STARTUP_TIMEOUT = 120;
    private static final DockerComposeContainer<?> composeContainer = new DockerComposeContainer<>(
            new File("src/test/resources/docker-compose.yml")).withStartupTimeout(
                    Duration.of(MAX_STARTUP_TIMEOUT, ChronoUnit.SECONDS))
            .withExposedService("minio-service", 9000)
            .withExposedService("tika-pipes", 50051);
    private static final String MINIO_ENDPOINT = "http://localhost:9000";
    private static final String ACCESS_KEY = "minio";
    private static final String SECRET_KEY = "minio123";
    private static final String FETCH_BUCKET = "fetch-bucket";
    private static final String EMIT_BUCKET = "emit-bucket";

    private static final Region REGION = Region.US_WEST_2;

    private S3Client s3Client;

    private final File testFileFolder = new File("target", "test-files");
    private final Set<String> testFiles = new HashSet<>();

    private void createTestFiles() {
        if (testFileFolder.mkdirs()) {
            LOG.info("Created test folder: {}", testFileFolder);
        }
        int numDocs = 42;
        for (int i = 0; i < numDocs; ++i) {
            String nextFileName = "test-" + i + ".html";
            testFiles.add(nextFileName);
            s3Client.putObject(PutObjectRequest.builder()
                    .bucket(FETCH_BUCKET)
                    .key(nextFileName)
                    .build(), RequestBody.fromString("<html><body>body-of-" + nextFileName + "</body></html>"));
        }
    }

    @BeforeAll
    void setupMinio() {
        composeContainer.start();
        initializeS3Client();
    }

    @AfterAll
    void closeMinio() {
        composeContainer.close();
    }

    private void initializeS3Client() {
        s3Client = S3Client.builder()
                .region(REGION)
                .endpointOverride(URI.create(MINIO_ENDPOINT))
                .forcePathStyle(true)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY)))
                .build();
    }

    @Test
    void s3PipelineIteratorS3FetcherAndS3Emitter() throws Exception {
        String fetcherId = UUID.randomUUID().toString();
        // create s3 bucket for fetches and for emits
        s3Client.createBucket(CreateBucketRequest.builder().bucket(FETCH_BUCKET).build());
        s3Client.createBucket(CreateBucketRequest.builder().bucket(EMIT_BUCKET).build());

        // create some test files and insert into fetch bucket
        createTestFiles();

        int grpcPort = composeContainer.getServicePort("tika-pipes", 50051);
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(composeContainer.getServiceHost("tika-pipes", grpcPort), grpcPort)
                .usePlaintext()
                .directExecutor()
                .build();
        TikaGrpc.TikaBlockingStub blockingStub = TikaGrpc.newBlockingStub(channel);
        TikaGrpc.TikaStub tikaStub = TikaGrpc.newStub(channel);

        S3FetcherConfig s3FetcherConfig = new S3FetcherConfig();
        s3FetcherConfig.setFetcherId(fetcherId);
        s3FetcherConfig.setPluginId("s3-fetcher");
        s3FetcherConfig.setBucket(FETCH_BUCKET);
        s3FetcherConfig.setExtractUserMetadata(true);
        s3FetcherConfig.setSpoolToTemp(true);
        s3FetcherConfig.setRegion(REGION.id());
        s3FetcherConfig.setAccessKey(ACCESS_KEY);
        s3FetcherConfig.setSecretKey(SECRET_KEY);
        s3FetcherConfig.setEndpointOverride(MINIO_ENDPOINT);

        SaveFetcherReply reply = blockingStub.saveFetcher(SaveFetcherRequest
                .newBuilder()
                .setFetcherId(fetcherId)
                .setPluginId(s3FetcherConfig.getPluginId())
                .setFetcherConfigJson(OBJECT_MAPPER.writeValueAsString(s3FetcherConfig))
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
        for (String nextS3Key : testFiles) {
            requestStreamObserver.onNext(FetchAndParseRequest
                    .newBuilder()
                    .setFetcherId(fetcherId)
                    .setFetchKey(nextS3Key)
                    .setFetchMetadataJson(OBJECT_MAPPER.writeValueAsString(Map.of()))
                    .build());
        }
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
