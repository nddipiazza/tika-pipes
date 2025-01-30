package org.apache.tika.pipes.grpc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.tika.*;
import org.apache.tika.pipes.PipesResult;
import org.apache.tika.pipes.core.emitter.DefaultEmitterConfig;
import org.apache.tika.pipes.core.emitter.EmitOutput;
import org.apache.tika.pipes.core.emitter.Emitter;
import org.apache.tika.pipes.core.emitter.EmitterConfig;
import org.apache.tika.pipes.core.exception.TikaPipesException;
import org.apache.tika.pipes.core.iterators.DefaultPipeIteratorConfig;
import org.apache.tika.pipes.core.iterators.PipeInput;
import org.apache.tika.pipes.core.iterators.PipeIterator;
import org.apache.tika.pipes.core.iterators.PipeIteratorConfig;
import org.apache.tika.pipes.core.parser.ParseService;
import org.apache.tika.pipes.fetchers.core.DefaultFetcherConfig;
import org.apache.tika.pipes.fetchers.core.Fetcher;
import org.apache.tika.pipes.fetchers.core.FetcherConfig;
import org.apache.tika.pipes.job.JobStatus;
import org.apache.tika.pipes.repo.EmitterRepository;
import org.apache.tika.pipes.repo.FetcherRepository;
import org.apache.tika.pipes.repo.JobStatusRepository;
import org.apache.tika.pipes.repo.PipeIteratorRepository;
import org.jetbrains.annotations.NotNull;
import org.pf4j.PluginManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@GrpcService
@Service
@Slf4j
public class TikaServiceImpl extends TikaGrpc.TikaImplBase {
    private final ExecutorService executorService = Executors.newCachedThreadPool(new TikaRunnerThreadFactory());
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
    private JobStatusRepository jobStatusRepository;

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
        Map<String, Object> fetchMetadata = objectMapper.readValue(request.getFetchMetadataJson(), MAP_STRING_OBJ_TYPE_REF);
        InputStream inputStream = fetcher.fetch(fetcherConfigFromPluginManager, request.getFetchKey(), fetchMetadata, responseMetadata);
        FetchAndParseReply.Builder builder = FetchAndParseReply.newBuilder();
        builder.setStatus(PipesResult.STATUS.EMIT_SUCCESS.name());
        builder.setFetchKey(request.getFetchKey());

        Map<String, Object> addedMetadata = objectMapper.readValue(request.getAddedMetadataJson(), MAP_STRING_OBJ_TYPE_REF);

        for (Map<String, Object> metadata : parseService.parseDocument(inputStream)) {
            Metadata.Builder metadataBuilder = Metadata.newBuilder();
            putMetadataFields(metadata, metadataBuilder);
            putMetadataFields(addedMetadata, metadataBuilder);
            builder.addMetadata(metadataBuilder.build());
        }
        responseObserver.onNext(builder.build());
    }

    private void putMetadataFields(Map<String, Object> metadata, Metadata.Builder metadataBuilder) throws JsonProcessingException {
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            ValueList.Builder valueListBuilder = ValueList.newBuilder();
            buildValueList(entry.getValue(), valueListBuilder);
            metadataBuilder.putFields(entry.getKey(), valueListBuilder.build());
        }
    }

    private void buildValueList(Object entryValue, ValueList.Builder valueListBuilder) throws JsonProcessingException {
        Value.Builder valueBuilder = Value.newBuilder();
        if (entryValue instanceof String) {
            valueBuilder.setStringValue((String) entryValue);
        } else if (entryValue instanceof Integer) {
            valueBuilder.setIntValue((Integer) entryValue);
        } else if (entryValue instanceof Long) {
            valueBuilder.setIntValue((Long) entryValue);
        } else if (entryValue instanceof Short) {
            valueBuilder.setIntValue((Short) entryValue);
        } else if (entryValue instanceof Double) {
            valueBuilder.setDoubleValue((Double) entryValue);
        } else if (entryValue instanceof Float) {
            valueBuilder.setDoubleValue((Float) entryValue);
        } else if (entryValue instanceof Boolean) {
            valueBuilder.setBoolValue((Boolean) entryValue);
        } else if (!(entryValue instanceof Object[])) {
            valueBuilder.setStringValue((String.valueOf(entryValue)));
        }
        if (entryValue instanceof Object[] arrayOfObj) {
            for (Object o : arrayOfObj) {
                buildValueList(o, valueListBuilder);
            }
        } else {
            valueListBuilder.addValues(valueBuilder.build());
        }
    }

    private Class<? extends FetcherConfig> getFetcherConfigClassFromPluginManager(DefaultFetcherConfig fetcherConfig) {
        return pluginManager
                .getExtensionClasses(FetcherConfig.class, fetcherConfig.getPluginId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Could not find a fetcher class for " + fetcherConfig.getFetcherId()));
    }

    private Class<? extends PipeIteratorConfig> getPipeIteratorConfigClassFromPluginManager(DefaultPipeIteratorConfig pipeIteratorConfig) {
        return pluginManager
                .getExtensionClasses(PipeIteratorConfig.class, pipeIteratorConfig.getPluginId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Could not find a pipe iterator class for " + pipeIteratorConfig.getPipeIteratorId()));
    }

    private Class<? extends EmitterConfig> getEmitterConfigClassFromPluginManager(DefaultEmitterConfig emitterConfig) {
        return pluginManager
                .getExtensionClasses(EmitterConfig.class, emitterConfig.getPluginId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Could not find a emitter class for " + emitterConfig.getEmitterId()));
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

    @Override
    public void runPipeJob(RunPipeJobRequest request,
                           StreamObserver<RunPipeJobReply> responseObserver) {
        String jobId = UUID.randomUUID().toString();
        updateJobStatus(jobId, true, false, false);

        executorService.submit(() -> runPipeJobImpl(request, responseObserver, jobId));

        responseObserver.onNext(RunPipeJobReply.newBuilder().setPipeJobId(jobId).build());
        responseObserver.onCompleted();
    }

    private void updateJobStatus(String jobId, boolean isRunning, boolean hasError, boolean isCompleted) {
        jobStatusRepository.save(jobId, JobStatus.builder()
                .jobId(jobId)
                .running(isRunning)
                .hasError(hasError)
                .completed(isCompleted)
                .build());
    }

    private void runPipeJobImpl(RunPipeJobRequest request, StreamObserver<RunPipeJobReply> responseObserver, String jobId) {
        try {
            DefaultPipeIteratorConfig pipeIteratorConfig = pipeIteratorRepository.findByPipeIteratorId(request.getPipeIteratorId());
            PipeIteratorConfig pipeIteratorConfigFromPluginManager = objectMapper.convertValue(pipeIteratorConfig.getConfig(), getPipeIteratorConfigClassFromPluginManager(pipeIteratorConfig));
            PipeIterator pipeIterator = getPipeIterator(pipeIteratorConfig.getPluginId());
            pipeIterator.init(pipeIteratorConfigFromPluginManager);
            DefaultEmitterConfig emitterConfig = emitterRepository.findByEmitterId(request.getEmitterId());
            EmitterConfig emitterConfigFromPluginManager = objectMapper.convertValue(emitterConfig.getConfig(), getEmitterConfigClassFromPluginManager(emitterConfig));
            Emitter emitter = getEmitter(emitterConfig.getPluginId());
            emitter.init(emitterConfigFromPluginManager);
            CountDownLatch countDownLatch = new CountDownLatch(1);
            StreamObserver<FetchAndParseRequest> requestStreamObserver = fetchAndParseBiDirectionalStreaming(new StreamObserver<>() {
                        @Override
                        public void onNext(FetchAndParseReply fetchAndParseReply) {
                            try {
                                List<Map<String, Object>> listOfMetadata = listOfMetadataToListOfMap(fetchAndParseReply);
                                emitter.emit(List.of(EmitOutput.builder()
                                        .fetchKey(fetchAndParseReply.getFetchKey())
                                        .metadata(listOfMetadata)
                                        .build()));
                            } catch (IOException e) {
                                log.error("Error emitting fetch key {}",
                                        fetchAndParseReply.getFetchKey(), e);
                                updateJobStatus(jobId, true, false, false);
                                responseObserver.onError(e);
                            }
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            log.error("Error streaming fetch and parse replies", throwable);
                            countDownLatch.countDown();
                        }

                        @Override
                        public void onCompleted() {
                            log.info("Finished streaming fetch and parse replies");
                            countDownLatch.countDown();
                        }
                    });

            while (pipeIterator.hasNext()) {
                List<PipeInput> pipeInputs = pipeIterator.next();

                for (PipeInput pipeInput : pipeInputs) {
                    requestStreamObserver.onNext(FetchAndParseRequest.newBuilder()
                            .setFetcherId(request.getFetcherId())
                            .setFetchKey(pipeInput.getFetchKey())
                            .setFetchMetadataJson(objectMapper.writeValueAsString(pipeInput.getMetadata()))
                            .setAddedMetadataJson("{}")
                            .build());
                }
            }
            requestStreamObserver.onCompleted();
            try {
                if (!countDownLatch.await(request.getJobCompletionTimeoutSeconds(), TimeUnit.SECONDS)) {
                    throw new TikaPipesException("Timed out waiting for job to complete");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            updateJobStatus(jobId, false, false, true);
        } catch (Throwable e) {
            log.error("Exception running pipe job", e);
            updateJobStatus(jobId, false, true, true);
        }
    }

    @NotNull
    private static List<Map<String, Object>> listOfMetadataToListOfMap(FetchAndParseReply fetchAndParseReply) {
        List<Map<String, Object>> listOfMetadata = new ArrayList<>();
        for (Metadata metadata : fetchAndParseReply.getMetadataList()) {
            Map<String, Object> metadataMap = new HashMap<>();
            for (String key : metadata.getFieldsMap().keySet()) {
                metadataMap.put(key, metadata.getFieldsMap().get(key));
            }
            listOfMetadata.add(metadataMap);
        }
        return listOfMetadata;
    }

    @Override
    public void getPipeJob(GetPipeJobRequest request,
                           StreamObserver<GetPipeJobReply> responseObserver) {
        JobStatus jobStatus = jobStatusRepository.findByJobId(request.getPipeJobId());
        if (jobStatus == null) {
            responseObserver.onError(
                    new IllegalArgumentException("Job ID not found: " + request.getPipeJobId()));
            return;
        }

        GetPipeJobReply.Builder replyBuilder =
                GetPipeJobReply.newBuilder().setPipeJobId(jobStatus.getJobId())
                        .setIsRunning(jobStatus.getRunning())
                        .setIsCompleted(jobStatus.getCompleted())
                        .setHasError(jobStatus.getHasError());

        responseObserver.onNext(replyBuilder.build());
        responseObserver.onCompleted();
    }

    private static class TikaRunnerThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        @Override
        public Thread newThread(@NotNull Runnable runnable) {
            String namePrefix = "pipes-runner-";
            return new Thread(runnable, namePrefix + threadNumber.getAndIncrement());
        }
    }
}
