package org.apache.tika.pipes.fetchers.s3;

import org.apache.commons.lang3.StringUtils;
import org.apache.tika.pipes.fetchers.s3.config.S3FetcherConfig;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

import java.net.URI;

public class S3ClientManager {

    private final ThreadLocal<S3Client> s3ClientThreadLocal;
    private final S3FetcherConfig s3FetcherConfig;

    public S3ClientManager(S3FetcherConfig s3FetcherConfig) {
        this.s3FetcherConfig = s3FetcherConfig;
        this.s3ClientThreadLocal = ThreadLocal.withInitial(this::initialize);
    }

    private S3Client initialize() {
        String credentialsProvider = s3FetcherConfig.getCredentialsProvider();
        AwsCredentialsProvider provider;
        if (credentialsProvider.equals("instance")) {
            provider = InstanceProfileCredentialsProvider.create();
        } else if (credentialsProvider.equals("profile")) {
            provider = ProfileCredentialsProvider.create(s3FetcherConfig.getProfile());
        } else if (credentialsProvider.equals("key_secret")) {
            provider = StaticCredentialsProvider.create(AwsSessionCredentials.create(s3FetcherConfig.getAccessKey(), s3FetcherConfig.getSecretKey(), s3FetcherConfig.getSessionToken()));
        } else {
            throw new IllegalArgumentException("credentialsProvider must be set and must be either 'instance', 'profile' or 'key_secret'");
        }

        ClientOverrideConfiguration clientConfiguration = ClientOverrideConfiguration.builder().build();
        S3ClientBuilder s3ClientBuilder = S3Client.builder()
                .overrideConfiguration(clientConfiguration)
                .credentialsProvider(provider)
                .region(Region.of(s3FetcherConfig.getRegion()));

        if (!StringUtils.isBlank(s3FetcherConfig.getEndpointConfigurationService())) {
            s3ClientBuilder.endpointOverride(URI.create(s3FetcherConfig.getEndpointConfigurationService()));
        }

        return s3ClientBuilder.build();
    }

    public S3Client getS3Client() {
        return s3ClientThreadLocal.get();
    }
}
