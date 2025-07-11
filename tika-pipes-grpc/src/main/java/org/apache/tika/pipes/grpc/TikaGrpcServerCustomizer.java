package org.apache.tika.pipes.grpc;

import grpcstarter.server.GrpcServerCustomizer;
import io.grpc.ServerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;

@Component
public class TikaGrpcServerCustomizer implements GrpcServerCustomizer {
    @Value("${grpc.server.numThreads}")
    private int numThreads;

    @Override
    public void customize(ServerBuilder<?> serverBuilder) {
        serverBuilder.executor(Executors.newFixedThreadPool(numThreads));
    }
}
