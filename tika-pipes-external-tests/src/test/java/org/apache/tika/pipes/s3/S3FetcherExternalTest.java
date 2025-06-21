package org.apache.tika.pipes.s3;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.FetchAndParseReply;
import org.apache.tika.FetchAndParseRequest;
import org.apache.tika.SaveFetcherReply;
import org.apache.tika.SaveFetcherRequest;
import org.apache.tika.TikaGrpc;
import org.apache.tika.pipes.ExternalTestBase;
import org.apache.tika.pipes.fetchers.s3.config.S3FetcherConfig;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Slf4j
class S3FetcherExternalTest extends ExternalTestBase {
    static final String ACCESS_KEY = "minio";
    static final String SECRET_KEY = "minio123";
    static final String FETCH_BUCKET = "fetch-bucket";
    static final String EMIT_BUCKET = "emit-bucket";

    static final Region REGION = Region.US_WEST_2;

    static S3Client s3Client;

    static final Set<String> testFiles = new HashSet<>();

    @BeforeAll
    static void initializeS3Client() {
        String minIoEndpoint = "http://" + composeContainer.getServiceHost("minio-service", 9000) + ":" + composeContainer.getServicePort("minio-service", 9000);
        s3Client = S3Client.builder()
                .region(REGION)
                .endpointOverride(URI.create(minIoEndpoint))
                .forcePathStyle(true)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY)))
                .build();
    }

    static private void copyGovdocs1ToBucket() {
        for (File corpaDistFile : Objects.requireNonNull(Paths.get("target", "govdocs1").toFile().listFiles())) {
            if (!corpaDistFile.isDirectory()) {
                continue;
            }
            for (File corpaFile : Objects.requireNonNull(corpaDistFile.listFiles())) {
                if (corpaFile.isFile()) {
                    testFiles.add(corpaFile.getName());
                    try (InputStream is = new FileInputStream(corpaFile)) {
                        s3Client.putObject(PutObjectRequest.builder()
                                .bucket(FETCH_BUCKET)
                                .key(corpaDistFile.getName() + "/" + corpaFile.getName())
                                .build(), RequestBody.fromInputStream(is, corpaFile.length()));
                        log.info("Uploaded file: {} to bucket: {}", corpaFile.getName(), FETCH_BUCKET);
                    } catch (IOException e) {
                        log.error("Error uploading file: {}", corpaFile.getName(), e);
                    }
                }
            }
        }
    }

    @Test
    void s3PipelineIteratorS3FetcherAndS3Emitter() throws Exception {
        String fetcherId = UUID.randomUUID().toString();
        // create s3 bucket for fetches and for emits
        s3Client.createBucket(CreateBucketRequest.builder().bucket(FETCH_BUCKET).build());
        s3Client.createBucket(CreateBucketRequest.builder().bucket(EMIT_BUCKET).build());

        // create some test files and insert into fetch bucket
        copyGovdocs1ToBucket();

        int grpcPort = composeContainer.getServicePort("tika-pipes", 9090);
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(composeContainer.getServiceHost("tika-pipes", grpcPort), grpcPort)
                .usePlaintext()
                .directExecutor()
                .maxInboundMessageSize(160 * 1024 * 1024) // 160 MB
                .build();
        TikaGrpc.TikaBlockingStub blockingStub = TikaGrpc.newBlockingStub(channel);
        TikaGrpc.TikaStub tikaStub = TikaGrpc.newStub(channel);

        S3FetcherConfig s3FetcherConfig = getS3FetcherConfig(fetcherId);

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

    @NotNull
    private static S3FetcherConfig getS3FetcherConfig(String fetcherId) {
        S3FetcherConfig s3FetcherConfig = new S3FetcherConfig();
        s3FetcherConfig.setFetcherId(fetcherId);
        s3FetcherConfig.setCredentialsProvider("key_secret");
        s3FetcherConfig.setPluginId("s3-fetcher");
        s3FetcherConfig.setBucket(FETCH_BUCKET);
        s3FetcherConfig.setExtractUserMetadata(true);
        s3FetcherConfig.setSpoolToTemp(true);
        s3FetcherConfig.setRegion(REGION.id());
        s3FetcherConfig.setAccessKey(ACCESS_KEY);
        s3FetcherConfig.setSecretKey(SECRET_KEY);
        s3FetcherConfig.setEndpointOverride("http://minio-service:9000");
        return s3FetcherConfig;
    }
}
