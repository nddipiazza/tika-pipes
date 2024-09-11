package org.apache.tika.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.InetAddress;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.apache.tika.GetFetcherReply;
import org.apache.tika.GetFetcherRequest;
import org.apache.tika.SaveFetcherReply;
import org.apache.tika.SaveFetcherRequest;
import org.apache.tika.TikaGrpc;

@SpringBootTest
@Testcontainers
class TikaPipesApplicationTests {
	@Container
	static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0");
	@Autowired
	ObjectMapper objectMapper;
	@Value("${grpc.server.port}")
	Integer port;

	@DynamicPropertySource
	static void mongoProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
	}

	String fetcherId = "http-fetcher-example";
	String pluginId = "http-fetcher";

	@Test
	void fetchersCrud() throws Exception {
		ManagedChannel channel = ManagedChannelBuilder.forAddress(InetAddress.getLocalHost().getHostAddress(), port) // Ensure the port is correct
													  .usePlaintext()
													  .build();
		TikaGrpc.TikaBlockingStub tikaBlockingStub = TikaGrpc.newBlockingStub(channel);

		SaveFetcherReply saveFetcherReply = tikaBlockingStub.saveFetcher(SaveFetcherRequest
				.newBuilder()
				.setFetcherId(fetcherId)
				.setPluginId(pluginId)
				.setFetcherConfigJson(objectMapper.writeValueAsString(ImmutableMap
						.builder()
						.build()))
				.build());
		assertEquals(fetcherId, saveFetcherReply.getFetcherId());
		GetFetcherReply getFetcherReply = tikaBlockingStub.getFetcher(GetFetcherRequest.newBuilder()
																	 .setFetcherId(fetcherId).build());
		assertEquals(fetcherId, getFetcherReply.getFetcherId());
	}
}
