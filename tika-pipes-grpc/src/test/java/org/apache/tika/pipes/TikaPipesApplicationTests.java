package org.apache.tika.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.net.InetAddress;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.apache.tika.DeleteFetcherReply;
import org.apache.tika.DeleteFetcherRequest;
import org.apache.tika.GetFetcherReply;
import org.apache.tika.GetFetcherRequest;
import org.apache.tika.ListFetchersReply;
import org.apache.tika.ListFetchersRequest;
import org.apache.tika.SaveFetcherReply;
import org.apache.tika.SaveFetcherRequest;
import org.apache.tika.TikaGrpc;
import org.apache.tika.pipes.fetcher.fs.config.FileSystemFetcherConfig;

@SpringBootTest
@ContextConfiguration(initializers = TikaPipesApplicationTests.DynamicPortInitializer.class)
class TikaPipesApplicationTests {
    @Autowired
    ObjectMapper objectMapper;
    @Value("${grpc.server.port}")
    Integer port;

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        System.setProperty("pf4j.mode", "development");
    }

    static class DynamicPortInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            int dynamicPort = findAvailablePort();
            TestPropertyValues
                    .of("grpc.server.port=" + dynamicPort)
                    .applyTo(applicationContext.getEnvironment());
            TestPropertyValues
                    .of("ignite.workDir=" + new File("target/" + UUID.randomUUID()).getAbsolutePath())
                    .applyTo(applicationContext.getEnvironment());
            File tikaPipesParentFolder = getTikaPipesParentFolder();
            TestPropertyValues
                    .of("plugins.pluginDirs=" + new File(tikaPipesParentFolder, "tika-pipes-fetchers").getAbsolutePath())
                    .applyTo(applicationContext.getEnvironment());
        }

        private File getTikaPipesParentFolder() {
            File currentDir = new File(System.getProperty("user.dir"));
            while (currentDir != null && currentDir.isDirectory() && currentDir.exists() && !currentDir.getName().equals("tika-pipes")) {
                currentDir = currentDir.getParentFile();
            }
            return currentDir;
        }

        private int findAvailablePort() {
            try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
                return socket.getLocalPort(); // Dynamically find an available port
            } catch (Exception e) {
                throw new RuntimeException("Failed to find an available port", e);
            }
        }
    }

    @Test
    void fetchersCrud() throws Exception {
        String fetcherId1 = "filesystem-fetcher-example1";
        String fetcherId2 = "filesystem-fetcher-example2";
        String pluginId = "filesystem-fetcher";
        ManagedChannel channel = ManagedChannelBuilder.forAddress(InetAddress
                                                              .getLocalHost()
                                                              .getHostAddress(), port) // Ensure the port is correct
                                                      .usePlaintext()
                                                      .build();
        TikaGrpc.TikaBlockingStub tikaBlockingStub = TikaGrpc.newBlockingStub(channel);

        saveFetcher(tikaBlockingStub, fetcherId1, pluginId);
        saveFetcher(tikaBlockingStub, fetcherId2, pluginId);

        ListFetchersReply listFetchersReply = tikaBlockingStub.listFetchers(ListFetchersRequest
                .newBuilder()
                .build());
        Assertions.assertTrue(listFetchersReply.getGetFetcherRepliesCount() > 0);

        DeleteFetcherReply deleteFetcherReply = tikaBlockingStub.deleteFetcher(DeleteFetcherRequest
                .newBuilder()
                .setFetcherId(fetcherId1)
                .build());
        Assertions.assertTrue(deleteFetcherReply.getSuccess());

        deleteFetcherReply = tikaBlockingStub.deleteFetcher(DeleteFetcherRequest
                .newBuilder()
                .setFetcherId(fetcherId2)
                .build());
        Assertions.assertTrue(deleteFetcherReply.getSuccess());

        listFetchersReply = tikaBlockingStub.listFetchers(ListFetchersRequest
                .newBuilder()
                .build());
        Assertions.assertEquals(0, listFetchersReply.getGetFetcherRepliesCount(), "Not supposed to have any fetchers but found " + listFetchersReply.getGetFetcherRepliesList());

        deleteFetcherReply = tikaBlockingStub.deleteFetcher(DeleteFetcherRequest
                .newBuilder()
                .setFetcherId("asdfasdfasdfas")
                .build());
        Assertions.assertFalse(deleteFetcherReply.getSuccess());
    }

    private void saveFetcher(TikaGrpc.TikaBlockingStub tikaBlockingStub, String fetcherId, String pluginId) throws JsonProcessingException {
        FileSystemFetcherConfig fileSystemFetcherConfig = new FileSystemFetcherConfig();
        fileSystemFetcherConfig.setExtractFileSystemMetadata(true);
        fileSystemFetcherConfig.setBasePath("target");

        SaveFetcherReply saveFetcherReply = tikaBlockingStub.saveFetcher(SaveFetcherRequest
                .newBuilder()
                .setFetcherId(fetcherId)
                .setPluginId(pluginId)
                .setFetcherConfigJson(objectMapper.writeValueAsString(fileSystemFetcherConfig))
                .build());
        assertEquals(fetcherId, saveFetcherReply.getFetcherId());
        GetFetcherReply getFetcherReply = tikaBlockingStub.getFetcher(GetFetcherRequest
                .newBuilder()
                .setFetcherId(fetcherId)
                .build());
        assertEquals(fetcherId, getFetcherReply.getFetcherId());
    }
}
