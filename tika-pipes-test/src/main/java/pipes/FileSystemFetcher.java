package pipes;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
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
import org.apache.tika.pipes.PipesResult;
import org.apache.tika.pipes.fetchers.filesystem.FileSystemFetcherConfig;

@Slf4j
public class FileSystemFetcher {
    public static final String TIKA_SERVER_GRPC_DEFAULT_HOST = "localhost";
    public static final int TIKA_SERVER_GRPC_DEFAULT_PORT = 9090;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    @Parameter(names = {"--base-directory"}, description = "Base directory", required = true)
    private File baseDirectory;

    @Parameter(names = {"--grpcHost"}, description = "The grpc host", help = true)
    private String host = TIKA_SERVER_GRPC_DEFAULT_HOST;

    @Parameter(names = {"--grpcPort"}, description = "The grpc server port", help = true)
    private Integer port = TIKA_SERVER_GRPC_DEFAULT_PORT;

    @Parameter(names = {"--fetcher-id"}, description = "What fetcher ID should we use? By default will use file-system-fetcher")
    private String fetcherId = "file-system-fetcher";

    @Parameter(names = {"-h", "-H", "--help"}, description = "Display help menu")
    private boolean help;

    public static void main(String[] args) throws IOException {
        FileSystemFetcher bulkParser = new FileSystemFetcher();
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

        FileSystemFetcherConfig fileSystemFetcherConfig = new FileSystemFetcherConfig();
        fileSystemFetcherConfig.setExtractFileSystemMetadata(true);
        fileSystemFetcherConfig.setBasePath(baseDirectory.getAbsolutePath());

        SaveFetcherReply reply = blockingStub.saveFetcher(SaveFetcherRequest
                .newBuilder()
                .setFetcherId(fetcherId)
                .setPluginId("filesystem-fetcher")
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
                if (PipesResult.STATUS.FETCH_EXCEPTION
                        .name()
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
                log.info("Finished streaming fetch and parse replies");
                countDownLatch.countDown();
            }
        });

        Files.walkFileTree(baseDirectory.toPath(), new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                try {
                    requestStreamObserver.onNext(FetchAndParseRequest
                            .newBuilder()
                            .setFetcherId(fetcherId)
                            .setFetchKey(file
                                    .toAbsolutePath()
                                    .toString())
                            .setMetadataJson(OBJECT_MAPPER.writeValueAsString(Map.of()))
                            .build());
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                System.out.println("Directory: " + dir.toString());
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                System.err.println("Failed to access file: " + file.toString() + " due to " + exc.getMessage());
                return FileVisitResult.CONTINUE;
            }
        });

        log.info("Done submitting fetch keys to {}", fetcherId);
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
