package org.apache.tika.pipes;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.apache.tika.SaveFetcherRequest;
import org.apache.tika.TikaGrpc;

@SpringBootTest
class TikaPipesApplicationTests {
	ManagedChannel channel;

	@Autowired
	ObjectMapper objectMapper;

	@BeforeEach
	void setupGrpc() throws IOException {
		channel = ManagedChannelBuilder.forAddress("localhost", 9090) // Ensure the port is correct
													  .usePlaintext()
													  .build();
	}

	@AfterEach
	public void tearDown() {
		// Shutdown the channel after each test
		channel.shutdown();
	}

	String fetcherId = "http-fetcher-example";
	String pluginId = "http-fetcher";

	@Test
	void fetchersCrud() {
		TikaGrpc.TikaBlockingStub tikaBlockingStub = TikaGrpc.newBlockingStub(channel);
		tikaBlockingStub.saveFetcher(new SaveFetcherRequest(SaveFetcherRequest
				.newBuilder()
				.setFetcherId(fetcherId)
				.setPluginId(pluginId)
				.setFetcherConfigJson(objectMapper.writeValueAsString(ImmutableMap
						.builder()
						.put("basePath", targetFolder)
						.put("extractFileSystemMetadata", true)
						.build()))
				.build()));
	}
}
