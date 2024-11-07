package org.apache.tika.pipes.grpc;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.pf4j.PluginManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

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
import org.apache.tika.Metadata;
import org.apache.tika.SaveFetcherReply;
import org.apache.tika.SaveFetcherRequest;
import org.apache.tika.TikaGrpc;
import org.apache.tika.pipes.PipesResult;
import org.apache.tika.pipes.core.exception.TikaPipesException;
import org.apache.tika.pipes.core.fetcher.Fetcher;
import org.apache.tika.pipes.core.fetcher.FetcherConfig;
import org.apache.tika.pipes.core.fetcher.FetcherConfigThreadLocal;
import org.apache.tika.pipes.core.parser.ParseService;
import org.apache.tika.pipes.repo.FetcherRepository;

@GrpcService
@Service
@Slf4j
public class TikaServiceImpl extends TikaGrpc.TikaImplBase {
    public static final TypeReference<Map<String, Object>> MAP_STRING_OBJ_TYPE_REF = new TypeReference<>() {
    };
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FetcherRepository fetcherRepository;

    @Autowired
    private Environment environment;

    @Autowired
    private ParseService parseService;

    @Autowired
    private PluginManager pluginManager;

    private Fetcher getFetcher(String pluginId) {
        return pluginManager
                .getExtensions(Fetcher.class, pluginId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new TikaPipesException("Could not find Fetcher extension for plugin " + pluginId));
    }

    @Override
    public void saveFetcher(SaveFetcherRequest request, StreamObserver<SaveFetcherReply> responseObserver) {
        try {
            FetcherConfig fetcherConfig = new FetcherConfig()
                    .setFetcherId(request.getFetcherId())
                    .setPluginId(request.getPluginId())
                    .setConfig(objectMapper.readValue(request
                            .getFetcherConfigJsonBytes()
                            .toByteArray(), new TypeReference<>() {
                    }));
            fetcherRepository.save(fetcherConfig.getFetcherId(), fetcherConfig);
            responseObserver.onNext(SaveFetcherReply
                    .newBuilder()
                    .setFetcherId(request.getFetcherId())
                    .build());
        } catch (IOException e) {
            responseObserver.onError(e);
        }
        responseObserver.onCompleted();
    }

    @Override
    public void getFetcher(GetFetcherRequest request, StreamObserver<GetFetcherReply> responseObserver) {
        FetcherConfig fetcherConfig = fetcherRepository.findByFetcherId(request.getFetcherId());
        responseObserver.onNext(GetFetcherReply
                .newBuilder()
                .setFetcherId(request.getFetcherId())
                .setPluginId(fetcherConfig.getPluginId())
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void listFetchers(ListFetchersRequest request, StreamObserver<ListFetchersReply> responseObserver) {
        ListFetchersReply.Builder builder = ListFetchersReply.newBuilder();
        fetcherRepository
                .findAll()
                .forEach(fetcherConfig -> builder.addGetFetcherReplies(GetFetcherReply
                        .newBuilder()
                        .setFetcherId(fetcherConfig.getFetcherId())
                        .setPluginId(fetcherConfig.getPluginId())
                        .build()));
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void deleteFetcher(DeleteFetcherRequest request, StreamObserver<DeleteFetcherReply> responseObserver) {
        boolean exists = fetcherRepository.existsByFetcherId(request.getFetcherId());
        if (exists) {
            fetcherRepository.deleteByFetcherId(request.getFetcherId());
        }
        responseObserver.onNext(DeleteFetcherReply
                .newBuilder()
                .setSuccess(exists)
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void fetchAndParse(FetchAndParseRequest request, StreamObserver<FetchAndParseReply> responseObserver) {
        try {
            fetchAndParseImpl(request, responseObserver);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    private void fetchAndParseImpl(FetchAndParseRequest request, StreamObserver<FetchAndParseReply> responseObserver) throws IOException {
        FetcherConfig fetcherConfig = fetcherRepository.findByFetcherId(request.getFetcherId());
        if (fetcherConfig == null) {
            throw new IOException("Could not find fetcher with ID " + request.getFetcherId());
        }
        FetcherConfigThreadLocal.setFetcherConfig(fetcherConfig);
        Fetcher fetcher = getFetcher(fetcherConfig.getPluginId());
        HashMap<String, Object> responseMetadata = new HashMap<>();
        InputStream inputStream = fetcher.fetch(request.getFetchKey(), objectMapper.readValue(request.getMetadataJson(), MAP_STRING_OBJ_TYPE_REF), responseMetadata);
        FetchAndParseReply.Builder builder = FetchAndParseReply.newBuilder();
        builder.setStatus(PipesResult.STATUS.EMIT_SUCCESS.name());
        builder.setFetchKey(request.getFetchKey());

        for (Map<String, Object> metadata : parseService.parseDocument(inputStream)) {
            Metadata.Builder metadataBuilder = Metadata.newBuilder();
            metadata.forEach((k, v) -> metadataBuilder.putFields(k, String.valueOf(v)));
            builder.addMetadata(metadataBuilder.build());
        }
        responseObserver.onNext(builder.build());
    }

    @Override
    public void fetchAndParseServerSideStreaming(FetchAndParseRequest request, StreamObserver<FetchAndParseReply> responseObserver) {
        try {
            fetchAndParseImpl(request, responseObserver);
            responseObserver.onCompleted();
        } catch (IOException e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public StreamObserver<FetchAndParseRequest> fetchAndParseBiDirectionalStreaming(StreamObserver<FetchAndParseReply> responseObserver) {
        return new StreamObserver<>() {
            @Override
            public void onNext(FetchAndParseRequest fetchAndParseRequest) {
                try {
                    fetchAndParseImpl(fetchAndParseRequest, responseObserver);
                } catch (IOException e) {
                    responseObserver.onError(e);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                log.error("Parse error occurred", throwable);
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public void getFetcherConfigJsonSchema(GetFetcherConfigJsonSchemaRequest request, StreamObserver<GetFetcherConfigJsonSchemaReply> responseObserver) {
        super.getFetcherConfigJsonSchema(request, responseObserver);
    }
}
