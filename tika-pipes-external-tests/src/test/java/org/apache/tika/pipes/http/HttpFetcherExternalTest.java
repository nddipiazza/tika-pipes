package org.apache.tika.pipes.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.FetchAndParseReply;
import org.apache.tika.FetchAndParseRequest;
import org.apache.tika.SaveFetcherReply;
import org.apache.tika.SaveFetcherRequest;
import org.apache.tika.TikaGrpc;
import org.apache.tika.pipes.ExternalTestBase;
import org.apache.tika.pipes.fetchers.http.config.HttpFetcherConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Slf4j
class HttpFetcherExternalTest extends ExternalTestBase {
    @Test
    void httpFetcher() throws Exception {
        String fetcherId = UUID.randomUUID().toString();
        ManagedChannel channel = getManagedChannel();
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
        StreamObserver<FetchAndParseRequest>
                requestStreamObserver = tikaStub.fetchAndParseBiDirectionalStreaming(new StreamObserver<>() {
            @Override
            public void onNext(FetchAndParseReply fetchAndParseReply) {
                log.debug("Reply from fetch-and-parse - key={}, metadata={}", fetchAndParseReply.getFetchKey(), fetchAndParseReply.getMetadataList());
                if ("FETCH_AND_PARSE_EXCEPTION".equals(fetchAndParseReply.getStatus())) {
                    errors.add(fetchAndParseReply);
                } else {
                    successes.add(fetchAndParseReply);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                log.error("Received an error", throwable);
                Assertions.fail(throwable);
                countDownLatch.countDown();
            }

            @Override
            public void onCompleted() {
                log.info("Finished streaming fetch and parse replies");
                countDownLatch.countDown();
            }
        });

        String baseUrl = "http://web";
        try (Stream<Path> paths = Files.walk(testFolder.toPath())) {
            paths.filter(Files::isRegularFile)
                    .forEach(file -> {
                        try {
                            requestStreamObserver.onNext(FetchAndParseRequest
                                    .newBuilder()
                                    .setFetcherId(fetcherId)
                                    .setFetchKey(baseUrl + "/" + testFolder.toPath().relativize(file))
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
    }
}
