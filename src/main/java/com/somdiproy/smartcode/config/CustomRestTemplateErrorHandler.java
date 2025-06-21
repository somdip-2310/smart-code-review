package com.somdiproy.smartcode.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * Custom Error Handler for RestTemplate
 * 
 * Provides better error handling and logging for HTTP requests
 * 
 * @author Somdip Roy
 */
public class CustomRestTemplateErrorHandler extends DefaultResponseErrorHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomRestTemplateErrorHandler.class);
    
    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        HttpStatus statusCode = HttpStatus.valueOf(response.getStatusCode().value());
        String statusText = response.getStatusText();
        String body = extractResponseBodyAsString(response);
        
        logger.error("HTTP Error: {} {} - Body: {}", statusCode.value(), statusText, body);
        
        switch (statusCode.series()) {
            case CLIENT_ERROR:
                throw new HttpClientErrorException(
                    statusCode, 
                    statusText, 
                    response.getHeaders(), 
                    body.getBytes(StandardCharsets.UTF_8), 
                    StandardCharsets.UTF_8
                );
                
            case SERVER_ERROR:
                throw new HttpServerErrorException(
                    statusCode, 
                    statusText, 
                    response.getHeaders(), 
                    body.getBytes(StandardCharsets.UTF_8), 
                    StandardCharsets.UTF_8
                );
                
            default:
                throw new RestClientException("Unknown status code: " + statusCode);
        }
    }
    
    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        HttpStatus statusCode = HttpStatus.valueOf(response.getStatusCode().value());
        return statusCode.series() == HttpStatus.Series.CLIENT_ERROR || 
               statusCode.series() == HttpStatus.Series.SERVER_ERROR;
    }
    
    /**
     * Extract response body as string for error logging
     */
    private String extractResponseBodyAsString(ClientHttpResponse response) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            logger.warn("Could not read error response body", e);
            return "[Could not read response body]";
        }
    }
}