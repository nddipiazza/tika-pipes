package org.apache.tika.pipes;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.FetchAndParseReply;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 *
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
@Slf4j
@Tag("ExternalTest")
public abstract class ExternalTestBase {
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    public static final int MAX_STARTUP_TIMEOUT = 120;
    public static final String govDocsFolder = "/tika/govdocs1"; // Volume specified in docker-compose.yml
    public static final File testFolder = new File("target", "govdocs1");
    public static final int govDocsFromIdx = Integer.parseInt(System.getProperty("govdocs1.fromIndex", "1"));
    public static final int govDocsToIdx = Integer.parseInt(System.getProperty("govdocs1.toIndex", "1"));
    public static final DockerComposeContainer<?> composeContainer = new DockerComposeContainer<>(
            new File("src/test/resources/docker-compose.yml"))
            .withEnv("HOST_GOVDOCS1_DIR", testFolder.getAbsolutePath())
            .withStartupTimeout(Duration.of(MAX_STARTUP_TIMEOUT, ChronoUnit.SECONDS))
            .withExposedService("tika-pipes", 9090)
            .withExposedService("minio-service", 9000)
            .withExposedService("web", 80)
            .withLogConsumer("tika-pipes", new Slf4jLogConsumer(log));
    public static final String DIGITAL_CORPA_ZIP_FILES_URL = "https://corp.digitalcorpora.org/corpora/files/govdocs1/zipfiles";

    @BeforeAll
    static void setup() throws Exception {
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
                TimeUnit.SECONDS.sleep(10);
            }
        }
        composeContainer.start();
    }

    @AfterAll
    void close() throws IOException {
        composeContainer.close();
        //FileUtils.deleteDirectory(testFolder);
    }

    public static void downloadAndUnzipGovdocs1(int fromIndex, int toIndex) throws IOException {
        Path targetDir = testFolder.toPath();
        Files.createDirectories(targetDir);

        for (int i = fromIndex; i <= toIndex; i++) {
            String zipName = String.format("%03d.zip", i);
            String url = DIGITAL_CORPA_ZIP_FILES_URL + "/" + zipName;
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

    public static void assertAllFilesFetched(Path baseDir, List<FetchAndParseReply> successes, List<FetchAndParseReply> errors) {
        Set<String> allFetchKeys = new HashSet<>();
        for (FetchAndParseReply reply : successes) {
            allFetchKeys.add(reply.getFetchKey());
        }
        for (FetchAndParseReply reply : errors) {
            allFetchKeys.add(reply.getFetchKey());
        }
        Set<String> keysFromGovdocs1 = new HashSet<>();
        try (Stream<Path> paths = Files.walk(baseDir)) {
            paths.filter(Files::isRegularFile)
                    .forEach(file -> {
                        String relPath = baseDir.relativize(file).toString();
                        if (Pattern.compile("\\d{3}\\.zip").matcher(relPath).find()) {
                            // Skip zip files
                            return;
                        }
                        keysFromGovdocs1.add(relPath);
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Assertions.assertNotEquals(0, successes.size(), "Should have been some successful fetches");
        Assertions.assertNotEquals(0, errors.size(), "Should have been some failed fetches");
        Assertions.assertEquals(keysFromGovdocs1, allFetchKeys, () -> {
            Set<String> missing = new HashSet<>(keysFromGovdocs1);
            missing.removeAll(allFetchKeys);
            return "Missing fetch keys: " + missing;
        });
    }

    public static ManagedChannel getManagedChannel() {
        return ManagedChannelBuilder
                .forAddress(composeContainer.getServiceHost("tika-pipes", 9090), composeContainer.getServicePort("tika-pipes", 9090))
                .usePlaintext()
                .executor(Executors.newCachedThreadPool())
                .maxInboundMessageSize(160 * 1024 * 1024) // 160 MB
                .build();
    }
}
