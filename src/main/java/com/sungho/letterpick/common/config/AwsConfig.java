package com.sungho.letterpick.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
public class AwsConfig {

    @Bean
    public S3Client s3Client(@Value("${letterpick.aws.region}") String region) {
        return S3Client.builder()
                .region(Region.of(region))
                .build();
    }

    @Bean
    public SqsClient sqsClient(@Value("${letterpick.aws.region}") String region) {
        return SqsClient.builder()
                .region(Region.of(region))
                .build();
    }
}
