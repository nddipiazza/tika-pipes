//package pipes;
//
//import java.io.File;
//import java.io.FileReader;
//import java.io.IOException;
//import java.nio.file.Files;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
//import java.util.Objects;
//
//import com.azure.identity.ClientCertificateCredentialBuilder;
//import com.beust.jcommander.JCommander;
//import com.beust.jcommander.Parameter;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.microsoft.graph.models.ListItem;
//import com.microsoft.graph.models.ListItemCollectionResponse;
//import com.microsoft.graph.models.Site;
//import com.microsoft.graph.models.SiteCollectionResponse;
//import com.microsoft.graph.serviceclient.GraphServiceClient;
//import io.grpc.ManagedChannel;
//import io.grpc.ManagedChannelBuilder;
//import io.grpc.stub.StreamObserver;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.io.FileUtils;
//import org.apache.commons.io.LineIterator;
//
//import org.apache.tika.FetchAndParseReply;
//import org.apache.tika.FetchAndParseRequest;
//import org.apache.tika.GetFetcherConfigJsonSchemaRequest;
//import org.apache.tika.SaveFetcherReply;
//import org.apache.tika.SaveFetcherRequest;
//import org.apache.tika.TikaGrpc;
//import org.apache.tika.pipes.PipesResult;
//import org.apache.tika.pipes.fetchers.microsoftgraph.MicrosoftGraphFetcher;
//import org.apache.tika.pipes.fetchers.microsoftgraph.config.MicrosoftGraphFetcherConfig;
//
//@Slf4j
//public class MicrosoftGraphGprcBulkParser {
//    public static final String TIKA_SERVER_GRPC_DEFAULT_HOST = "localhost";
//    public static final int TIKA_SERVER_GRPC_DEFAULT_PORT = 50052;
//    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
//    public static final String MS_GRAPH_FETCHER_PLUGIN_ID = MicrosoftGraphFetcher.class.getName();
//
//    @Parameter(names = {"--grpcHost"}, description = "The grpc host", help = true)
//    private String host = TIKA_SERVER_GRPC_DEFAULT_HOST;
//
//    @Parameter(names = {"--grpcPort"}, description = "The grpc server port", help = true)
//    private Integer port = TIKA_SERVER_GRPC_DEFAULT_PORT;
//
//    @Parameter(names = {"--input-keys-csv"}, description = "CSV files with input files to fetch and parse. Each line contains siteId,itemId")
//    private File inputKeysCsv;
//
//    @Parameter(names = {"--microsoftGraphCredentialsCertificateKey"}, description = "The PFX key for client certificate authentication for the ms graph fetcher", help = true)
//    private File microsoftGraphClientCertificate;
//
//    @Parameter(names = {"--microsoftGraphCredentialsCertificatePassword"}, description = "The password for the PFX key for client certificate authentication for the ms graph fetcher", help = true, password = true)
//    private String microsoftGraphClientCertificatePassword;
//
//    @Parameter(names = {"--microsoftGraphCredentialsClientId"}, description = "The MS graph client ID")
//    private String microsoftGraphCredentialsClientId;
//
//    @Parameter(names = {"--microsoftGraphCredentialsTenantId"}, description = "The MS graph tenant ID")
//    private String microsoftGraphCredentialsTenantId;
//
//    @Parameter(names = {"--scope"}, description = "OAuth 2.0 scopes to add to the ms graph api calls")
//    private List<String> scopes;
//
//    @Parameter(names = {"-s", "--secure"}, description = "Enable credentials required to access this grpc server")
//    private boolean secure = false;
//
//    @Parameter(names = {"--cert-chain"}, description = "Certificate chain file. Example: server1.pem See: https://github.com/grpc/grpc-java/tree/b3ffb5078df361d7460786e134db7b5c00939246/examples/example-tls")
//    private File certChain;
//
//    @Parameter(names = {"--private-key"}, description = "Private key store. Example: server1.key See: https://github.com/grpc/grpc-java/tree/b3ffb5078df361d7460786e134db7b5c00939246/examples/example-tls")
//    private File privateKey;
//
//    @Parameter(names = {"--private-key-password"}, description = "Private key password, if needed")
//    private String privateKeyPassword;
//
//    @Parameter(names = {"--trust-cert-collection"}, description = "The trust certificate collection (root certs). Example: ca.pem See: https://github.com/grpc/grpc-java/tree/b3ffb5078df361d7460786e134db7b5c00939246/examples/example-tls")
//    private File trustCertCollection;
//
//    @Parameter(names = {"--client-auth-required"}, description = "Is Mutual TLS required?")
//    private boolean clientAuthRequired;
//
//    @Parameter(names = {"--fetcher-id"}, description = "What fetcher ID should we use? By default will use ms-graph-fetcher")
//    private String fetcherId = "ms-graph-fetcher";
//
//    @Parameter(names = {"-h", "-H", "--help"}, description = "Display help menu")
//    private boolean help;
//
//    private GraphServiceClient graphClient;
//
//    public static void main(String[] args) throws IOException {
//        MicrosoftGraphGprcBulkParser microsoftGraphGprcBulkParser = new MicrosoftGraphGprcBulkParser();
//        JCommander commander = JCommander
//                .newBuilder()
//                .addObject(microsoftGraphGprcBulkParser)
//                .build();
//
//        commander.parse(args);
//
//        if (microsoftGraphGprcBulkParser.help) {
//            commander.usage();
//            return;
//        }
//
//        microsoftGraphGprcBulkParser.runFetch();
//    }
//
//    private void runFetch() throws IOException {
//        ManagedChannel channel = ManagedChannelBuilder
//                .forAddress(host, port)
//                .usePlaintext()
//                .directExecutor()
//                .build();
//        TikaGrpc.TikaBlockingStub blockingStub = TikaGrpc.newBlockingStub(channel);
//        TikaGrpc.TikaStub tikaStub = TikaGrpc.newStub(channel);
//        MicrosoftGraphFetcherConfig microsoftGraphFetcherConfig = new MicrosoftGraphFetcherConfig();
//        microsoftGraphFetcherConfig.setClientCertificateCredentialsConfig(new ClientCertificateCredentialsConfig()
//                .setCertificateBytes(FileUtils.readFileToByteArray(microsoftGraphClientCertificate))
//                .setCertificatePassword(microsoftGraphClientCertificatePassword)
//                .setClientId(microsoftGraphCredentialsClientId)
//                .setTenantId(microsoftGraphCredentialsTenantId));
//        microsoftGraphFetcherConfig.setScopes(scopes);
//
//        log.info("MS graph fetcher json schema: {}", blockingStub
//                .getFetcherConfigJsonSchema(GetFetcherConfigJsonSchemaRequest
//                        .newBuilder()
//                        .setPluginId(MicrosoftGraphFetcherConfig.PLUGIN_ID)
//                        .build())
//                .getFetcherConfigJsonSchema());
//
//        graphClient = new GraphServiceClient(new ClientCertificateCredentialBuilder()
//                .clientId(microsoftGraphCredentialsClientId)
//                .tenantId(microsoftGraphCredentialsTenantId)
//                .pfxCertificate(Files.newInputStream(microsoftGraphClientCertificate.toPath()))
//                .clientCertificatePassword(microsoftGraphClientCertificatePassword)
//                .build(), scopes.toArray(new String[]{}));
//
//        SiteCollectionResponse siteCollectionResponse = graphClient
//                .sites()
//                .withUrl("https://atoliotech.sharepoint.com/sites/testit")
//                .get();
//
//        Site site = graphClient
//                .sites()
//                .bySiteId("atoliotech.sharepoint.com,12d075eb-c98b-4df2-8cc9-ea1e2f5a75bc,52618207-6069-4ebd-bb94-b28cf29be61d")
//                .get();
//
//
//        SaveFetcherReply reply = blockingStub.saveFetcher(SaveFetcherRequest
//                .newBuilder()
//                .setFetcherId(fetcherId)
//                .setPluginId(MS_GRAPH_FETCHER_PLUGIN_ID)
//                .setFetcherConfigJson(OBJECT_MAPPER.writeValueAsString(microsoftGraphFetcherConfig))
//                .build());
//        log.info("Saved fetcher with ID {}", reply.getFetcherId());
//
//        List<FetchAndParseReply> successes = Collections.synchronizedList(new ArrayList<>());
//        List<FetchAndParseReply> errors = Collections.synchronizedList(new ArrayList<>());
//
//        StreamObserver<FetchAndParseRequest> requestStreamObserver = tikaStub.fetchAndParseBiDirectionalStreaming(new StreamObserver<>() {
//            @Override
//            public void onNext(FetchAndParseReply fetchAndParseReply) {
//                log.debug("Reply from fetch-and-parse - key={}, metadata={}", fetchAndParseReply.getFetchKey(), fetchAndParseReply.getFieldsMap());
//                if (PipesResult.STATUS.FETCH_EXCEPTION
//                        .name()
//                        .equals(fetchAndParseReply.getStatus())) {
//                    errors.add(fetchAndParseReply);
//                } else {
//                    successes.add(fetchAndParseReply);
//                }
//            }
//
//            @Override
//            public void onError(Throwable throwable) {
//                log.error("Received an error", throwable);
//            }
//
//            @Override
//            public void onCompleted() {
//                log.info("Finished streaming fetch and parse replies");
//            }
//        });
//
//        if (inputKeysCsv != null && inputKeysCsv.exists()) {
//            log.info("Input keys file found: {}", inputKeysCsv);
//            try (LineIterator lineIterator = new LineIterator(new FileReader(inputKeysCsv))) {
//                while (lineIterator.hasNext()) {
//                    String nextLine = lineIterator.nextLine();
//                    log.info("Request fetch-and-parse: {}", nextLine);
//                    requestStreamObserver.onNext(FetchAndParseRequest
//                            .newBuilder()
//                            .setFetchKey(nextLine)
//                            .build());
//                }
//            }
//        }
//        log.info("Done submitting fetch keys to {}", fetcherId);
//        requestStreamObserver.onCompleted();
//    }
//
//    private void listItemsInSite(String siteId) {
//        Site site = graphClient
//                .sites()
//                .bySiteId(siteId)
//                .get();
//
//        assert site != null;
//        System.out.println("Site ID: " + site.getId());
//        System.out.println("Site Name: " + site.getDisplayName());
//
//        // List items in the site's root
//		ListItemCollectionResponse listItemCollectionResponse = graphClient
//                .sites()
//				 .bySiteId(siteId)
//				 .lists()
//				 .byListId("")
//				 .items()
//				.get();
//
//        assert listItemCollectionResponse != null;
//        for (ListItem item : Objects.requireNonNull(listItemCollectionResponse.getValue())) {
//            System.out.println("Item ID: " + item.getId());
//            System.out.println("Item Name: " + item.getName());
//        }
//    }
//}
