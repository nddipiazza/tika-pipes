package org.apache.tika.pipes.fetchers.googledrive;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSSessionCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.apache.commons.lang3.StringUtils;

import org.apache.tika.pipes.fetchers.googledrive.config.S3FetcherConfig;

public class S3ClientManager {

    private final ThreadLocal<AmazonS3> s3ClientThreadLocal;
    private final S3FetcherConfig s3FetcherConfig;

    public S3ClientManager(S3FetcherConfig s3FetcherConfig) {
        this.s3FetcherConfig = s3FetcherConfig;
        this.s3ClientThreadLocal = ThreadLocal.withInitial(this::initialize);
    }

    private AmazonS3 initialize() {
        String credentialsProvider = s3FetcherConfig.getCredentialsProvider();
        AWSCredentialsProvider provider;
        if (credentialsProvider.equals("instance")) {
            provider = InstanceProfileCredentialsProvider.getInstance();
        } else if (credentialsProvider.equals("profile")) {
            provider = new ProfileCredentialsProvider(s3FetcherConfig.getProfile());
        } else if (credentialsProvider.equals("key_secret")) {
            provider = new AWSStaticCredentialsProvider(new AWSSessionCredentials() {
                @Override
                public String getSessionToken() {
                    return s3FetcherConfig.getSessionToken();
                }

                @Override
                public String getAWSAccessKeyId() {
                    return s3FetcherConfig.getAccessKey();
                }

                @Override
                public String getAWSSecretKey() {
                    return s3FetcherConfig.getSecretKey();
                }
            });
        } else {
            throw new IllegalArgumentException("credentialsProvider must be set and must be either 'instance', 'profile' or 'key_secret'");
        }

        ClientConfiguration clientConfiguration = new ClientConfiguration().withMaxConnections(s3FetcherConfig.getMaxConnections());
        AmazonS3ClientBuilder amazonS3ClientBuilder = AmazonS3ClientBuilder
                .standard()
                .withClientConfiguration(clientConfiguration)
                .withPathStyleAccessEnabled(s3FetcherConfig.isPathStyleAccessEnabled())
                .withCredentials(provider);

        if (!StringUtils.isBlank(s3FetcherConfig.getEndpointConfigurationService())) {
            amazonS3ClientBuilder.setEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(s3FetcherConfig.getEndpointConfigurationService(), s3FetcherConfig.getRegion()));
        } else {
            amazonS3ClientBuilder.withRegion(s3FetcherConfig.getRegion());
        }

        return amazonS3ClientBuilder.build();
    }

    public AmazonS3 getS3Client() {
        return s3ClientThreadLocal.get();
    }
}
