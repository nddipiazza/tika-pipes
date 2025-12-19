package org.apache.tika.pipes.examples;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.apache.tika.GetFetcherReply;
import org.apache.tika.GetFetcherRequest;
import org.apache.tika.ListFetchersReply;
import org.apache.tika.ListFetchersRequest;
import org.apache.tika.SaveFetcherReply;
import org.apache.tika.SaveFetcherRequest;
import org.apache.tika.TikaGrpc;
import org.apache.tika.pipes.fetcher.fs.config.FileSystemFetcherConfig;

/**
 * Example demonstrating configuration synchronization over gRPC without requiring Apache Ignite.
 * 
 * This example shows how multiple clients can save and retrieve configurations
 * from the Tika Pipes server using the in-memory configuration store.
 */
public class ConfigSyncExample {
    
    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 9090;
        
        // Create a gRPC channel to the Tika Pipes server
        ManagedChannel channel = ManagedChannelBuilder
            .forAddress(host, port)
            .usePlaintext()
            .build();
        
        TikaGrpc.TikaBlockingStub stub = TikaGrpc.newBlockingStub(channel);
        ObjectMapper objectMapper = new ObjectMapper();
        
        try {
            // Example 1: Save a fetcher configuration
            System.out.println("=== Saving Fetcher Configuration ===");
            FileSystemFetcherConfig config = new FileSystemFetcherConfig();
            config.setBasePath("/data/documents");
            config.setExtractFileSystemMetadata(true);
            
            SaveFetcherRequest saveRequest = SaveFetcherRequest.newBuilder()
                .setFetcherId("my-filesystem-fetcher")
                .setPluginId("filesystem-fetcher")
                .setFetcherConfigJson(objectMapper.writeValueAsString(config))
                .build();
            
            SaveFetcherReply saveReply = stub.saveFetcher(saveRequest);
            System.out.println("Saved fetcher: " + saveReply.getFetcherId());
            
            // Example 2: Retrieve the fetcher configuration
            System.out.println("\n=== Retrieving Fetcher Configuration ===");
            GetFetcherRequest getRequest = GetFetcherRequest.newBuilder()
                .setFetcherId("my-filesystem-fetcher")
                .build();
            
            GetFetcherReply getReply = stub.getFetcher(getRequest);
            System.out.println("Retrieved fetcher: " + getReply.getFetcherId());
            System.out.println("Plugin ID: " + getReply.getPluginId());
            
            // Example 3: List all fetchers
            System.out.println("\n=== Listing All Fetchers ===");
            ListFetchersRequest listRequest = ListFetchersRequest.newBuilder().build();
            ListFetchersReply listReply = stub.listFetchers(listRequest);
            
            System.out.println("Total fetchers: " + listReply.getGetFetcherRepliesCount());
            listReply.getGetFetcherRepliesList().forEach(fetcher -> {
                System.out.println("  - " + fetcher.getFetcherId() + " (plugin: " + fetcher.getPluginId() + ")");
            });
            
            // Example 4: Simulate another client retrieving the same configuration
            System.out.println("\n=== Simulating Another Client ===");
            System.out.println("Client 2 retrieving the configuration saved by Client 1...");
            
            GetFetcherReply client2Reply = stub.getFetcher(getRequest);
            System.out.println("Client 2 successfully retrieved: " + client2Reply.getFetcherId());
            System.out.println("This demonstrates config sync over gRPC without Ignite!");
            
        } finally {
            channel.shutdown();
        }
    }
}
