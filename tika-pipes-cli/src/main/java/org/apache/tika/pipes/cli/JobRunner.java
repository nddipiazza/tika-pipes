package org.apache.tika.pipes.cli;

import java.io.File;

import com.beust.jcommander.Parameter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JobRunner {
    public static final String TIKA_SERVER_GRPC_DEFAULT_HOST = "localhost";
    public static final int TIKA_SERVER_GRPC_DEFAULT_PORT = 50051;
    @Parameter(names = {"--base-directory"}, description = "Base directory", required = true)
    private File baseDirectory;
    @Parameter(names = {"--grpcHost"}, description = "The grpc host", help = true)
    private String host = TIKA_SERVER_GRPC_DEFAULT_HOST;
    @Parameter(names = {"--grpcPort"}, description = "The grpc server port", help = true)
    private Integer port = TIKA_SERVER_GRPC_DEFAULT_PORT;
    @Parameter(names = {"-h", "-H", "--help"}, description = "Display help menu")
    private boolean help;


}
