package edu.aitutor.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

/**
 * S3客户端配置（用于MinIO）
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class S3Config {

    private final StorageConfigProperties storageConfig;
    
    @Bean
    public S3Client s3Client() {
        String accessKey = storageConfig.getAccessKey();
        log.info("初始化 S3 客户端: Endpoint={}, Bucket={}, AccessKey={}****", 
            storageConfig.getEndpoint(), 
            storageConfig.getBucket(),
            accessKey != null && accessKey.length() > 3 ? accessKey.substring(0, 3) : "null");

        AwsBasicCredentials credentials = AwsBasicCredentials.create(
            storageConfig.getAccessKey(),
            storageConfig.getSecretKey()
        );
        
        return S3Client.builder()
            .endpointOverride(URI.create(storageConfig.getEndpoint()))
            .region(Region.of(storageConfig.getRegion()))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .forcePathStyle(true)
            .build();
    }
}
