package com.somdiproy.smartcode.config;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
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
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClientBuilder;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import java.time.Duration;

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
    
    @Value("${aws.endpoint.url:}")
    private String endpointUrl;
    
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
    
    /**
     * Create AWS SDK v1 Client Configuration with clock skew handling
     */
    private ClientConfiguration createClientConfiguration() {
        ClientConfiguration clientConfig = new ClientConfiguration();
        
        // Connection settings
        clientConfig.setConnectionTimeout(10000); // 10 seconds
        clientConfig.setSocketTimeout(10000); // 10 seconds
        clientConfig.setRequestTimeout(30000); // 30 seconds
        clientConfig.setClientExecutionTimeout(60000); // 60 seconds
        
        // Retry settings with automatic clock skew adjustment
        clientConfig.setMaxErrorRetry(5); // Increased retries for clock skew
        clientConfig.setUseThrottleRetries(true);
        
        // Protocol settings
        clientConfig.setProtocol(Protocol.HTTPS);
        clientConfig.setUseExpectContinue(false);
        
        // Connection pooling
        clientConfig.setMaxConnections(50);
        clientConfig.setConnectionMaxIdleMillis(60000);
        clientConfig.setValidateAfterInactivityMillis(5000);
        
        // Enable compression for better performance
        clientConfig.setUseGzip(true);
        
        // Additional settings for better clock skew handling
        clientConfig.setUseTcpKeepAlive(true);
        
        logger.info("Created AWS Client Configuration with clock skew handling");
        return clientConfig;
    }
    
    @Bean
    public AmazonSQS amazonSQS(AWSCredentialsProvider credentialsProvider) {
        logger.info("Creating AmazonSQS client with clock skew configuration");
        
        AmazonSQSClientBuilder builder = AmazonSQSClientBuilder.standard()
                .withCredentials(credentialsProvider)
                .withRegion(region)
                .withClientConfiguration(createClientConfiguration());
        
        // Support for LocalStack or custom endpoints
        if (!endpointUrl.isEmpty()) {
            logger.info("Using custom endpoint: {}", endpointUrl);
            builder.withEndpointConfiguration(
                new AwsClientBuilder.EndpointConfiguration(endpointUrl, region)
            );
        }
        
        return builder.build();
    }
    
    @Bean
    public AmazonDynamoDB amazonDynamoDB(AWSCredentialsProvider credentialsProvider) {
        logger.info("Creating AmazonDynamoDB client with clock skew configuration");
        
        AmazonDynamoDBClientBuilder builder = AmazonDynamoDBClientBuilder.standard()
                .withCredentials(credentialsProvider)
                .withRegion(region)
                .withClientConfiguration(createClientConfiguration());
        
        // Support for LocalStack or custom endpoints
        if (!endpointUrl.isEmpty()) {
            logger.info("Using custom endpoint: {}", endpointUrl);
            builder.withEndpointConfiguration(
                new AwsClientBuilder.EndpointConfiguration(endpointUrl, region)
            );
        }
        
        return builder.build();
    }
    
    /**
     * Create AWS SDK v2 Client Override Configuration with retry and timeout settings
     */
    private ClientOverrideConfiguration createV2ClientConfiguration() {
        return ClientOverrideConfiguration.builder()
                .apiCallTimeout(Duration.ofSeconds(60))
                .apiCallAttemptTimeout(Duration.ofSeconds(30))
                .retryPolicy(RetryPolicy.builder()
                        .numRetries(5)
                        .build())
                .build();
    }
    
    @Bean
    public S3Client s3Client(AwsCredentialsProvider credentialsProvider) {
        logger.info("Creating S3Client with enhanced configuration");
        
        S3ClientBuilder builder = S3Client.builder()
                .credentialsProvider(credentialsProvider)
                .region(Region.of(region))
                .overrideConfiguration(createV2ClientConfiguration());
        
        // Support for LocalStack or custom endpoints
        if (!endpointUrl.isEmpty()) {
            logger.info("Using custom endpoint for S3: {}", endpointUrl);
            builder.endpointOverride(java.net.URI.create(endpointUrl));
        }
        
        return builder.build();
    }
    
    @Bean
    public BedrockRuntimeClient bedrockRuntimeClient(AwsCredentialsProvider credentialsProvider) {
        logger.info("Creating BedrockRuntimeClient with enhanced configuration");
        
        BedrockRuntimeClientBuilder builder = BedrockRuntimeClient.builder()
                .credentialsProvider(credentialsProvider)
                .region(Region.US_EAST_1)
                .overrideConfiguration(createV2ClientConfiguration());
        
        // Note: Bedrock doesn't support custom endpoints
        
        return builder.build();
    }
}