package com.somdiproy.smartcode.config;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@Profile("!test") 
public class AWSConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(AWSConfig.class);
    
    @Value("${aws.credentials.access-key:}")
    private String accessKey;
    
    @Value("${aws.credentials.secret-key:}")
    private String secretKey;
    
    @Value("${aws.credentials.use-default-chain:true}")
    private boolean useDefaultChain;
    
    @Value("${aws.region:us-east-1}")
    private String region;
    
    /**
     * AWS SDK v1 Credentials Provider
     */
    @Bean
    public AWSCredentialsProvider awsCredentialsProvider() {
        if (!useDefaultChain && !accessKey.isEmpty() && !secretKey.isEmpty()) {
            logger.info("Using configured AWS credentials");
            BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKey, secretKey);
            return new AWSStaticCredentialsProvider(awsCreds);
        } else {
            logger.info("Using default AWS credentials chain");
            return DefaultAWSCredentialsProviderChain.getInstance();
        }
    }
    
    /**
     * AWS SDK v2 Credentials Provider
     */
    @Bean
    public AwsCredentialsProvider awsV2CredentialsProvider() {
        if (!useDefaultChain && !accessKey.isEmpty() && !secretKey.isEmpty()) {
            logger.info("Using configured AWS credentials for SDK v2");
            AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKey, secretKey);
            return StaticCredentialsProvider.create(awsCreds);
        } else {
            logger.info("Using default AWS credentials chain for SDK v2");
            return DefaultCredentialsProvider.create();
        }
    }
    
    @Bean
    public AmazonSQS amazonSQS(AWSCredentialsProvider credentialsProvider) {
        return AmazonSQSClientBuilder.standard()
                .withCredentials(credentialsProvider)
                .withRegion(region)
                .build();
    }
    
    @Bean
    public AmazonDynamoDB amazonDynamoDB(AWSCredentialsProvider credentialsProvider) {
        return AmazonDynamoDBClientBuilder.standard()
                .withCredentials(credentialsProvider)
                .withRegion(region)
                .build();
    }
    
    @Bean
    public S3Client s3Client(AwsCredentialsProvider credentialsProvider) {
        return S3Client.builder()
                .credentialsProvider(credentialsProvider)
                .region(Region.of(region))
                .build();
    }
    
    @Bean
    public BedrockRuntimeClient bedrockRuntimeClient(AwsCredentialsProvider credentialsProvider) {
        return BedrockRuntimeClient.builder()
                .credentialsProvider(credentialsProvider)
                .region(Region.US_EAST_1)
                .build();
    }
}