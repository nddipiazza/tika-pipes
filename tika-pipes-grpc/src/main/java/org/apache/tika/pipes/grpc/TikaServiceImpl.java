package org.apache.tika.pipes.grpc;

import java.io.IOException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fetcher.FetcherConfig;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;

import org.apache.tika.DeleteFetcherReply;
import org.apache.tika.DeleteFetcherRequest;
import org.apache.tika.FetchAndParseReply;
import org.apache.tika.FetchAndParseRequest;
import org.apache.tika.GetFetcherConfigJsonSchemaReply;
import org.apache.tika.GetFetcherConfigJsonSchemaRequest;
import org.apache.tika.GetFetcherReply;
import org.apache.tika.GetFetcherRequest;
import org.apache.tika.ListFetchersReply;
import org.apache.tika.ListFetchersRequest;
import org.apache.tika.SaveFetcherReply;
import org.apache.tika.SaveFetcherRequest;
import org.apache.tika.TikaGrpc;
import org.apache.tika.pipes.repo.FetcherRepository;

@GrpcService
@Slf4j
public class TikaServiceImpl extends TikaGrpc.TikaImplBase {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private FetcherRepository fetcherRepository;

    @Override
    public void saveFetcher(SaveFetcherRequest request, StreamObserver<SaveFetcherReply> responseObserver) {
        try {
            FetcherConfig fetcherConfig = new FetcherConfig()
                    .setFetcherId(request.getFetcherId())
                    .setPluginId(request.getPluginId())
                    .setConfig(OBJECT_MAPPER.readValue(request
                            .getFetcherConfigJsonBytes()
                            .toByteArray(), new TypeReference<>() {}));
            fetcherRepository.save(fetcherConfig);
        } catch (IOException e) {
            responseObserver.onError(e);
        }
        responseObserver.onCompleted();
    }

    @Override
    public void getFetcher(GetFetcherRequest request, StreamObserver<GetFetcherReply> responseObserver) {
        FetcherConfig fetcherConfig = fetcherRepository.findByFetcherId(request.getFetcherId());
        log.info("Fetcher config: {}", fetcherConfig);
    }

    @Override
    public void listFetchers(ListFetchersRequest request, StreamObserver<ListFetchersReply> responseObserver) {
        super.listFetchers(request, responseObserver);
    }

    @Override
    public void deleteFetcher(DeleteFetcherRequest request, StreamObserver<DeleteFetcherReply> responseObserver) {
        super.deleteFetcher(request, responseObserver);
    }

    @Override
    public void fetchAndParse(FetchAndParseRequest request, StreamObserver<FetchAndParseReply> responseObserver) {
        super.fetchAndParse(request, responseObserver);
    }

    @Override
    public void fetchAndParseServerSideStreaming(FetchAndParseRequest request, StreamObserver<FetchAndParseReply> responseObserver) {
        super.fetchAndParseServerSideStreaming(request, responseObserver);
    }

    @Override
    public StreamObserver<FetchAndParseRequest> fetchAndParseBiDirectionalStreaming(StreamObserver<FetchAndParseReply> responseObserver) {
        return super.fetchAndParseBiDirectionalStreaming(responseObserver);
    }

    @Override
    public void getFetcherConfigJsonSchema(GetFetcherConfigJsonSchemaRequest request, StreamObserver<GetFetcherConfigJsonSchemaReply> responseObserver) {
        super.getFetcherConfigJsonSchema(request, responseObserver);
    }
}
