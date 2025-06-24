// ===== S3Service.java =====
package com.somdiproy.smartcode.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class S3Service {
    
    private static final Logger logger = LoggerFactory.getLogger(S3Service.class);
    
    @Value("${aws.s3.bucket-name:smartcode-uploads}")
    private String bucketName;
    
    @Value("${aws.s3.region:us-east-1}")
    private String region;
    
    private final S3Client s3Client;

    public S3Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    // Remove the getS3Client() method entirely
    
    public String uploadZipFile(MultipartFile file, String analysisId) {
        try {
            // Create a unique S3 key with proper structure
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String s3Key = String.format("uploads/%s/%s/%s", timestamp, analysisId, file.getOriginalFilename());
            
            logger.info("Uploading file to S3: {}/{}", bucketName, s3Key);
            
            // Upload directly from MultipartFile input stream
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();
            
            s3Client.putObject(putObjectRequest, 
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            
            logger.info("File uploaded successfully to S3: {}/{}", bucketName, s3Key);
            return s3Key;
            
        } catch (Exception e) {
            logger.error("Error uploading file to S3", e);
            throw new RuntimeException("Failed to upload file to S3", e);
        }
    }
    
	public String uploadFromInputStream(InputStream inputStream, long contentLength, String filename,
			String contentType, String analysisId) {
		try {
			String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
			String s3Key = String.format("uploads/%s/%s/%s", timestamp, analysisId, filename);

			logger.info("Uploading file to S3: {}/{}", bucketName, s3Key);

			PutObjectRequest putObjectRequest = PutObjectRequest.builder().bucket(bucketName).key(s3Key)
					.contentType(contentType).contentLength(contentLength).build();

			s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, contentLength));

			logger.info("File uploaded successfully to S3: {}/{}", bucketName, s3Key);
			return s3Key;

		} catch (Exception e) {
			logger.error("Error uploading file to S3", e);
			throw new RuntimeException("Failed to upload file to S3", e);
		}
	}
}