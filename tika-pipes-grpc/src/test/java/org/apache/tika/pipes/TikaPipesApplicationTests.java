package org.apache.tika.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.InetAddress;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.apache.tika.DeleteFetcherReply;
import org.apache.tika.DeleteFetcherRequest;
import org.apache.tika.GetFetcherReply;
import org.apache.tika.GetFetcherRequest;
import org.apache.tika.ListFetchersReply;
import org.apache.tika.ListFetchersRequest;
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


	@Test
	void fetchersCrud() throws Exception {
		String fetcherId1 = "http-fetcher-example1";
		String fetcherId2 = "http-fetcher-example2";
		String pluginId = "http-fetcher";
		ManagedChannel channel = ManagedChannelBuilder.forAddress(InetAddress.getLocalHost().getHostAddress(), port) // Ensure the port is correct
													  .usePlaintext()
													  .build();
		TikaGrpc.TikaBlockingStub tikaBlockingStub = TikaGrpc.newBlockingStub(channel);

		saveFetcher(tikaBlockingStub, fetcherId1, pluginId);
		saveFetcher(tikaBlockingStub, fetcherId2, pluginId);

		ListFetchersReply listFetchersReply = tikaBlockingStub.listFetchers(ListFetchersRequest.newBuilder().build());
		Assertions.assertEquals(2, listFetchersReply.getGetFetcherRepliesCount());

		DeleteFetcherReply deleteFetcherReply = tikaBlockingStub.deleteFetcher(DeleteFetcherRequest.newBuilder().setFetcherId(fetcherId1).build());
		Assertions.assertTrue(deleteFetcherReply.getSuccess());

		deleteFetcherReply = tikaBlockingStub.deleteFetcher(DeleteFetcherRequest.newBuilder().setFetcherId(fetcherId2).build());
		Assertions.assertTrue(deleteFetcherReply.getSuccess());

		listFetchersReply = tikaBlockingStub.listFetchers(ListFetchersRequest.newBuilder().build());
		Assertions.assertEquals(0, listFetchersReply.getGetFetcherRepliesCount());

		deleteFetcherReply = tikaBlockingStub.deleteFetcher(DeleteFetcherRequest.newBuilder().setFetcherId("asdfasdfasdfas").build());
		Assertions.assertFalse(deleteFetcherReply.getSuccess());

	}

	private void saveFetcher(TikaGrpc.TikaBlockingStub tikaBlockingStub, String fetcherId1, String pluginId) throws JsonProcessingException {
		SaveFetcherReply saveFetcherReply = tikaBlockingStub.saveFetcher(SaveFetcherRequest
				.newBuilder()
				.setFetcherId(fetcherId1)
				.setPluginId(pluginId)
				.setFetcherConfigJson(objectMapper.writeValueAsString(ImmutableMap
						.builder()
						.build()))
				.build());
		assertEquals(fetcherId1, saveFetcherReply.getFetcherId());
		GetFetcherReply getFetcherReply = tikaBlockingStub.getFetcher(GetFetcherRequest.newBuilder()
																					   .setFetcherId(fetcherId1).build());
		assertEquals(fetcherId1, getFetcherReply.getFetcherId());
	}
}
