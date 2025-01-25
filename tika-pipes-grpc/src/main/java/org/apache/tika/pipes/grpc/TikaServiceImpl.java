package org.apache.tika.pipes.grpc;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.apache.commons.lang3.NotImplementedException;
import org.pf4j.PluginManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import org.apache.tika.DeleteEmitterReply;
import org.apache.tika.DeleteEmitterRequest;
import org.apache.tika.DeleteFetcherReply;
import org.apache.tika.DeleteFetcherRequest;
import org.apache.tika.DeletePipeIteratorReply;
import org.apache.tika.DeletePipeIteratorRequest;
import org.apache.tika.FetchAndParseReply;
import org.apache.tika.FetchAndParseRequest;
import org.apache.tika.GetEmitterConfigJsonSchemaReply;
import org.apache.tika.GetEmitterConfigJsonSchemaRequest;
import org.apache.tika.GetEmitterReply;
import org.apache.tika.GetEmitterRequest;
import org.apache.tika.GetFetcherConfigJsonSchemaReply;
import org.apache.tika.GetFetcherConfigJsonSchemaRequest;
import org.apache.tika.GetFetcherReply;
import org.apache.tika.GetFetcherRequest;
import org.apache.tika.GetPipeIteratorConfigJsonSchemaReply;
import org.apache.tika.GetPipeIteratorConfigJsonSchemaRequest;
import org.apache.tika.GetPipeIteratorReply;
import org.apache.tika.GetPipeIteratorRequest;
import org.apache.tika.ListEmittersReply;
import org.apache.tika.ListEmittersRequest;
import org.apache.tika.ListFetchersReply;
import org.apache.tika.ListFetchersRequest;
import org.apache.tika.ListPipeIteratorsReply;
import org.apache.tika.ListPipeIteratorsRequest;
import org.apache.tika.Metadata;
import org.apache.tika.SaveEmitterReply;
import org.apache.tika.SaveEmitterRequest;
import org.apache.tika.SaveFetcherReply;
import org.apache.tika.SaveFetcherRequest;
import org.apache.tika.SavePipeIteratorReply;
import org.apache.tika.SavePipeIteratorRequest;
import org.apache.tika.TikaGrpc;
import org.apache.tika.pipes.PipesResult;
import org.apache.tika.pipes.core.emitter.DefaultEmitterConfig;
import org.apache.tika.pipes.core.emitter.Emitter;
import org.apache.tika.pipes.core.emitter.EmitterConfig;
import org.apache.tika.pipes.core.exception.TikaPipesException;
import org.apache.tika.pipes.core.iterators.DefaultPipeIteratorConfig;
import org.apache.tika.pipes.core.iterators.PipeIterator;
import org.apache.tika.pipes.core.iterators.PipeIteratorConfig;
import org.apache.tika.pipes.core.parser.ParseService;
import org.apache.tika.pipes.fetchers.core.DefaultFetcherConfig;
import org.apache.tika.pipes.fetchers.core.Fetcher;
import org.apache.tika.pipes.fetchers.core.FetcherConfig;
import org.apache.tika.pipes.repo.EmitterRepository;
import org.apache.tika.pipes.repo.FetcherRepository;
import org.apache.tika.pipes.repo.PipeIteratorRepository;

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
    private EmitterRepository emitterRepository;

    @Autowired
    private PipeIteratorRepository pipeIteratorRepository;

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

    private DefaultFetcherConfig newFetcherConfig(String pluginId, String fetcherId) {
        DefaultFetcherConfig fetcherConfig = new DefaultFetcherConfig();
        fetcherConfig.setPluginId(pluginId);
        fetcherConfig.setFetcherId(fetcherId);
        fetcherConfig.setConfig(objectMapper.convertValue(getFetcherConfig(pluginId), new TypeReference<>() {}));
        return fetcherConfig;
    }

    private FetcherConfig getFetcherConfig(String pluginId) {
        return pluginManager
                .getExtensions(FetcherConfig.class, pluginId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new TikaPipesException("Could not find FetcherConfig extension for plugin " + pluginId));
    }

    private Emitter getEmitter(String pluginId) {
        return pluginManager
                .getExtensions(Emitter.class, pluginId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new TikaPipesException("Could not find Emitter extension for plugin " + pluginId));
    }

    private EmitterConfig getEmitterConfig(String pluginId) {
        return pluginManager
                .getExtensions(EmitterConfig.class, pluginId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new TikaPipesException("Could not find EmitterConfig extension for plugin " + pluginId));
    }

    private DefaultEmitterConfig newEmitterConfig(String pluginId, String emitterId) {
        DefaultEmitterConfig emitterConfig = new DefaultEmitterConfig();
        emitterConfig.setPluginId(pluginId);
        emitterConfig.setEmitterId(emitterId);
        emitterConfig.setConfig(objectMapper.convertValue(getEmitterConfig(pluginId), new TypeReference<>() {}));
        return emitterConfig;
    }

    private PipeIterator getPipeIterator(String pluginId) {
        return pluginManager
                .getExtensions(PipeIterator.class, pluginId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new TikaPipesException("Could not find PipeIterator extension for plugin " + pluginId));
    }

    private PipeIteratorConfig getPipeIteratorConfig(String pluginId) {
        return pluginManager
                .getExtensions(PipeIteratorConfig.class, pluginId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new TikaPipesException("Could not find PipeIteratorConfig extension for plugin " + pluginId));
    }

    private DefaultPipeIteratorConfig newPipeIteratorConfig(String pluginId, String pipeIteratorId) {
        DefaultPipeIteratorConfig pipeIteratorConfig = new DefaultPipeIteratorConfig();
        pipeIteratorConfig.setPluginId(pluginId);
        pipeIteratorConfig.setPipeIteratorId(pipeIteratorId);
        pipeIteratorConfig.setConfig(objectMapper.convertValue(getPipeIteratorConfig(pluginId), new TypeReference<>() {}));
        return pipeIteratorConfig;
    }

    @Override
    public void saveFetcher(SaveFetcherRequest request, StreamObserver<SaveFetcherReply> responseObserver) {
        try {
            FetcherConfig fetcherConfig = newFetcherConfig(request.getPluginId(), request.getFetcherId());
            fetcherConfig.setFetcherId(request.getFetcherId())
                    .setPluginId(request.getPluginId())
                    .setConfig(objectMapper.readValue(request
                            .getFetcherConfigJsonBytes()
                            .toByteArray(), new TypeReference<>() {
                    }));
            fetcherRepository.save(fetcherConfig.getFetcherId(), newFetcherConfig(request));
            responseObserver.onNext(SaveFetcherReply
                    .newBuilder()
                    .setFetcherId(request.getFetcherId())
                    .build());
        } catch (IOException e) {
            responseObserver.onError(e);
        }
        responseObserver.onCompleted();
    }

    private DefaultFetcherConfig newFetcherConfig(SaveFetcherRequest request) throws JsonProcessingException {
        return new DefaultFetcherConfig()
                .setFetcherId(request.getFetcherId())
                .setPluginId(request.getPluginId())
                .setConfig(objectMapper.readValue(request.getFetcherConfigJson(), new TypeReference<>() {
                }));
    }

    @Override
    public void getFetcher(GetFetcherRequest request, StreamObserver<GetFetcherReply> responseObserver) {
        DefaultFetcherConfig fetcherConfig = fetcherRepository.findByFetcherId(request.getFetcherId());
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
        boolean exists = fetcherRepository.findByFetcherId(request.getFetcherId()) != null;
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
        DefaultFetcherConfig fetcherConfig = fetcherRepository.findByFetcherId(request.getFetcherId());
        if (fetcherConfig == null) {
            throw new IOException("Could not find fetcher with ID " + request.getFetcherId());
        }
        Fetcher fetcher = getFetcher(fetcherConfig.getPluginId());
        HashMap<String, Object> responseMetadata = new HashMap<>();
        // The fetcherRepository returns objects in the gprc server's classloader
        // But the fetcher.fetch will be done within the pf4j plugin.
        // If you send DefaultFetcherConfig and try to cast to the respective config you'll get a class loading error.
        // To get past this, get the correct class from the plugin manager, and convert to it.
        FetcherConfig fetcherConfigFromPluginManager = objectMapper.convertValue(fetcherConfig.getConfig(), getFetcherConfigClassFromPluginManager(fetcherConfig));
        InputStream inputStream = fetcher.fetch(fetcherConfigFromPluginManager, request.getFetchKey(), objectMapper.readValue(request.getMetadataJson(), MAP_STRING_OBJ_TYPE_REF), responseMetadata);
        FetchAndParseReply.Builder builder = FetchAndParseReply.newBuilder();
        builder.setStatus(PipesResult.STATUS.EMIT_SUCCESS.name());
        builder.setFetchKey(request.getFetchKey());

        for (Map<String, Object> metadata : parseService.parseDocument(inputStream)) {
            Metadata.Builder metadataBuilder = Metadata.newBuilder();
            metadata.forEach((key, val) -> metadataBuilder.putFields(key, String.valueOf(val)));
            builder.addMetadata(metadataBuilder.build());
        }
        responseObserver.onNext(builder.build());
    }

    private Class<? extends FetcherConfig> getFetcherConfigClassFromPluginManager(DefaultFetcherConfig fetcherConfig) {
        return pluginManager
                .getExtensionClasses(FetcherConfig.class, fetcherConfig.getPluginId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Could not find a fetcher class for " + fetcherConfig.getFetcherId()));
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
        throw new NotImplementedException();
    }

    @Override
    public void saveEmitter(SaveEmitterRequest request, StreamObserver<SaveEmitterReply> responseObserver) {
        try {
            DefaultEmitterConfig emitterConfig = newEmitterConfig(request.getPluginId(), request.getEmitterId());
            emitterConfig.setEmitterId(request.getEmitterId())
                         .setPluginId(request.getPluginId())
                         .setConfig(objectMapper.readValue(request.getEmitterConfigJson(), new TypeReference<>() {}));
            emitterRepository.save(emitterConfig.getEmitterId(), emitterConfig);
            responseObserver.onNext(SaveEmitterReply.newBuilder().setEmitterId(request.getEmitterId()).build());
        } catch (IOException e) {
            responseObserver.onError(e);
        }
        responseObserver.onCompleted();
    }

    @Override
    public void getEmitter(GetEmitterRequest request, StreamObserver<GetEmitterReply> responseObserver) {
        EmitterConfig emitterConfig = emitterRepository.findByEmitterId(request.getEmitterId());
        responseObserver.onNext(GetEmitterReply.newBuilder()
                                               .setEmitterId(request.getEmitterId())
                                               .setPluginId(emitterConfig.getPluginId())
                                               .build());
        responseObserver.onCompleted();
    }

    @Override
    public void listEmitters(ListEmittersRequest request, StreamObserver<ListEmittersReply> responseObserver) {
        ListEmittersReply.Builder builder = ListEmittersReply.newBuilder();
        emitterRepository.findAll().forEach(emitterConfig -> builder.addGetEmitterReplies(GetEmitterReply.newBuilder()
                                                                                                         .setEmitterId(emitterConfig.getEmitterId())
                                                                                                         .setPluginId(emitterConfig.getPluginId())
                                                                                                         .build()));
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void deleteEmitter(DeleteEmitterRequest request, StreamObserver<DeleteEmitterReply> responseObserver) {
        boolean exists = emitterRepository.findByEmitterId(request.getEmitterId()) != null;
        if (exists) {
            emitterRepository.deleteByEmitterId(request.getEmitterId());
        }
        responseObserver.onNext(DeleteEmitterReply.newBuilder().setSuccess(exists).build());
        responseObserver.onCompleted();
    }

    @Override
    public void getEmitterConfigJsonSchema(GetEmitterConfigJsonSchemaRequest request, StreamObserver<GetEmitterConfigJsonSchemaReply> responseObserver) {
        throw new NotImplementedException();
    }

    @Override
    public void savePipeIterator(SavePipeIteratorRequest request, StreamObserver<SavePipeIteratorReply> responseObserver) {
        try {
            DefaultPipeIteratorConfig pipeIteratorConfig = newPipeIteratorConfig(request.getPluginId(), request.getPipeIteratorId());
            pipeIteratorConfig.setPipeIteratorId(request.getPipeIteratorId())
                              .setPluginId(request.getPluginId())
                              .setConfig(objectMapper.readValue(request.getPipeIteratorConfigJson(), new TypeReference<>() {}));
            pipeIteratorRepository.save(pipeIteratorConfig.getPipeIteratorId(), pipeIteratorConfig);
            responseObserver.onNext(SavePipeIteratorReply.newBuilder().setPipeIteratorId(request.getPipeIteratorId()).build());
        } catch (IOException e) {
            responseObserver.onError(e);
        }
        responseObserver.onCompleted();
    }

    @Override
    public void getPipeIterator(GetPipeIteratorRequest request, StreamObserver<GetPipeIteratorReply> responseObserver) {
        PipeIteratorConfig pipeIteratorConfig = pipeIteratorRepository.findByPipeIteratorId(request.getPipeIteratorId());
        responseObserver.onNext(GetPipeIteratorReply.newBuilder()
                                                    .setPipeIteratorId(request.getPipeIteratorId())
                                                    .setPluginId(pipeIteratorConfig.getPluginId())
                                                    .build());
        responseObserver.onCompleted();
    }

    @Override
    public void listPipeIterators(ListPipeIteratorsRequest request, StreamObserver<ListPipeIteratorsReply> responseObserver) {
        ListPipeIteratorsReply.Builder builder = ListPipeIteratorsReply.newBuilder();
        pipeIteratorRepository.findAll().forEach(pipeIteratorConfig -> builder.addGetPipeIteratorReplies(GetPipeIteratorReply.newBuilder()
                                                                                                                             .setPipeIteratorId(pipeIteratorConfig.getPipeIteratorId())
                                                                                                                             .setPluginId(pipeIteratorConfig.getPluginId())
                                                                                                                             .build()));
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void deletePipeIterator(DeletePipeIteratorRequest request, StreamObserver<DeletePipeIteratorReply> responseObserver) {
        boolean exists = pipeIteratorRepository.findByPipeIteratorId(request.getPipeIteratorId()) != null;
        if (exists) {
            pipeIteratorRepository.deleteByPipeIteratorId(request.getPipeIteratorId());
        }
        responseObserver.onNext(DeletePipeIteratorReply.newBuilder().setSuccess(exists).build());
        responseObserver.onCompleted();
    }

    @Override
    public void getPipeIteratorConfigJsonSchema(GetPipeIteratorConfigJsonSchemaRequest request, StreamObserver<GetPipeIteratorConfigJsonSchemaReply> responseObserver) {
        throw new NotImplementedException();
    }
}
