package com.sungho.letterpick.common.config;

import io.awspring.cloud.autoconfigure.core.AwsConnectionDetails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.net.URI;

@Configuration
public class AwsConfig {

    @Bean
    public S3Client s3Client(@Value("${letterpick.aws.region}") String region) {
        return S3Client.builder()
                .region(Region.of(region))
                .build();
    }

    @Bean
    public SqsClient sqsClient(
            @Value("${spring.cloud.aws.region.static}") String region,
            @Value("${spring.cloud.aws.sqs.endpoint:}") String endpoint,
            ObjectProvider<AwsConnectionDetails> awsConnectionDetails
    ) {
        AwsConnectionDetails connectionDetails = awsConnectionDetails.getIfAvailable();
        String resolvedRegion = resolveRegion(region, connectionDetails);
        URI resolvedEndpoint = resolveEndpoint(endpoint, connectionDetails);

        var builder = SqsClient.builder()
                .region(Region.of(resolvedRegion));

        if (resolvedEndpoint != null) {
            builder.endpointOverride(resolvedEndpoint);
        }

        if (hasText(connectionDetails != null ? connectionDetails.getAccessKey() : null)
                && hasText(connectionDetails.getSecretKey())) {
            builder.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
                    connectionDetails.getAccessKey(),
                    connectionDetails.getSecretKey()
            )));
        }

        return builder.build();
    }

    private String resolveRegion(String configuredRegion, AwsConnectionDetails connectionDetails) {
        if (connectionDetails != null && hasText(connectionDetails.getRegion())) {
            return connectionDetails.getRegion();
        }
        return configuredRegion;
    }

    private URI resolveEndpoint(String configuredEndpoint, AwsConnectionDetails connectionDetails) {
        if (connectionDetails != null && connectionDetails.getEndpoint() != null) {
            return connectionDetails.getEndpoint();
        }
        if (hasText(configuredEndpoint)) {
            return URI.create(configuredEndpoint);
        }
        return null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
