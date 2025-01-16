package pipes.googledrive;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.LineIterator;

import org.apache.tika.FetchAndParseReply;
import org.apache.tika.FetchAndParseRequest;
import org.apache.tika.SaveFetcherReply;
import org.apache.tika.SaveFetcherRequest;
import org.apache.tika.TikaGrpc;
import org.apache.tika.pipes.fetchers.googledrive.config.GoogleDriveFetcherConfig;

@Slf4j
public class GoogleDriveFetcherExternalTest {
    public static final String TIKA_SERVER_GRPC_DEFAULT_HOST = "localhost";
    public static final int TIKA_SERVER_GRPC_DEFAULT_PORT = 9090;

    @Parameter(names = {"--fetch-urls"}, description = "List of URLs to fetch")
    private File urlsToFetchFile;

    @Parameter(names = {"--grpcHost"}, description = "The grpc host", help = true)
    private String host = TIKA_SERVER_GRPC_DEFAULT_HOST;

    @Parameter(names = {"--grpcPort"}, description = "The grpc server port", help = true)
    private Integer port = TIKA_SERVER_GRPC_DEFAULT_PORT;

    @Parameter(names = {"--fetcher-id"}, description = "What fetcher ID should we use? By default will use http-fetcher")
    private String fetcherId = "google-drive-fetcher";

    @Parameter(names = {"--help", "-h"}, help = true)
    private boolean help = false;

    @Parameter(names = {"--applicationName"}, description = "Google Drive application name")
    private String applicationName = "tika-pipes";

    @Parameter(names = {"--serviceAccountKeyBase64"}, description = "Service account creds file in base64", required = true)
    private String serviceAccountKeyBase64;

    @Parameter(names = {"--scopes"}, description = "Google Drive API scopes")
    private List<String> scopes = new ArrayList<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public GoogleDriveFetcherExternalTest() {
        objectMapper.registerModule(new GuavaModule());
    }

    public static void main(String[] args) throws Exception {
        GoogleDriveFetcherExternalTest bulkParser = new GoogleDriveFetcherExternalTest();
        JCommander commander = JCommander
                .newBuilder()
                .addObject(bulkParser)
                .build();
        commander.parse(args);
        if (bulkParser.help) {
            commander.usage();
            return;
        }
        bulkParser.runFetch();
    }

    private void runFetch() throws IOException {
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(host, port)
                .usePlaintext()
                .directExecutor()
                .build();
        TikaGrpc.TikaBlockingStub blockingStub = TikaGrpc.newBlockingStub(channel);
        TikaGrpc.TikaStub tikaStub = TikaGrpc.newStub(channel);

        GoogleDriveFetcherConfig googleDriveFetcherConfig = new GoogleDriveFetcherConfig();
        googleDriveFetcherConfig.setApplicationName(applicationName);
        googleDriveFetcherConfig.setScopes(scopes);
        googleDriveFetcherConfig.setServiceAccountKeyBase64(serviceAccountKeyBase64);

        SaveFetcherReply reply = blockingStub.saveFetcher(SaveFetcherRequest
                .newBuilder()
                .setFetcherId(fetcherId)
                .setPluginId("google-drive-fetcher")
                .setFetcherConfigJson(objectMapper.writeValueAsString(googleDriveFetcherConfig))
                .build());
        log.info("Saved fetcher with ID {}", reply.getFetcherId());

        List<FetchAndParseReply> successes = Collections.synchronizedList(new ArrayList<>());
        List<FetchAndParseReply> errors = Collections.synchronizedList(new ArrayList<>());

        CountDownLatch countDownLatch = new CountDownLatch(1);
        StreamObserver<FetchAndParseRequest> requestStreamObserver = tikaStub.fetchAndParseBiDirectionalStreaming(new StreamObserver<>() {
            @Override
            public void onNext(FetchAndParseReply fetchAndParseReply) {
                log.debug("Reply from fetch-and-parse - key={}, metadata={}", fetchAndParseReply.getFetchKey(), fetchAndParseReply.getMetadataList());
                if ("FetchException".equals(fetchAndParseReply.getStatus())) {
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
                log.info("Finished streaming fetch and parse replies");
                countDownLatch.countDown();
            }
        });

        try (LineIterator lineIterator = new LineIterator(new FileReader(urlsToFetchFile))) {
            while (lineIterator.hasNext()) {
                String nextS3Key = lineIterator.nextLine();
                requestStreamObserver.onNext(FetchAndParseRequest
                        .newBuilder()
                        .setFetcherId(fetcherId)
                        .setFetchKey(nextS3Key)
                        .setMetadataJson(objectMapper.writeValueAsString(Map.of()))
                        .build());
            }
        }
        log.info("Done submitting URLs to {}", fetcherId);
        requestStreamObserver.onCompleted();

        try {
            if (!countDownLatch.await(3, TimeUnit.MINUTES)) {
                log.error("Timed out waiting for parse to complete");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("Fetched: success={}", successes);
    }
}
