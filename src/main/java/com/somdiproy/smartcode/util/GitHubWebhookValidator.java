package com.somdiproy.smartcode.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * GitHub Webhook Signature Validator
 * 
 * Validates webhook signatures to ensure requests are from GitHub
 * 
 * @author Somdip Roy
 */
@Component
public class GitHubWebhookValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(GitHubWebhookValidator.class);
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";
    
    /**
     * Validate GitHub webhook signature
     * 
     * @param payload The webhook payload
     * @param signature The X-Hub-Signature-256 header value
     * @param secret The webhook secret
     * @return true if signature is valid
     */
    public boolean validateSignature(String payload, String signature, String secret) {
        if (payload == null || signature == null || secret == null) {
            logger.warn("Missing required parameters for signature validation");
            return false;
        }
        
        if (!signature.startsWith(SIGNATURE_PREFIX)) {
            logger.warn("Invalid signature format: {}", signature);
            return false;
        }
        
        try {
            String expectedSignature = SIGNATURE_PREFIX + calculateHmac(payload, secret);
            boolean isValid = constantTimeEquals(expectedSignature, signature);
            
            if (!isValid) {
                logger.warn("Signature mismatch - Expected: {}, Received: {}", 
                    maskSignature(expectedSignature), maskSignature(signature));
            }
            
            return isValid;
            
        } catch (Exception e) {
            logger.error("Error validating webhook signature", e);
            return false;
        }
    }
    
    /**
     * Calculate HMAC-SHA256 hash
     */
    private String calculateHmac(String data, String secret) 
            throws NoSuchAlgorithmException, InvalidKeyException {
        
        Mac mac = Mac.getInstance(HMAC_SHA256);
        SecretKeySpec secretKeySpec = new SecretKeySpec(
            secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256
        );
        mac.init(secretKeySpec);
        
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        
        // Convert to hex string
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        
        return hexString.toString();
    }
    
    /**
     * Constant time string comparison to prevent timing attacks
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        
        if (a.length() != b.length()) {
            return false;
        }
        
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        
        return result == 0;
    }
    
    /**
     * Mask signature for logging (security)
     */
    private String maskSignature(String signature) {
        if (signature == null || signature.length() < 20) {
            return "***";
        }
        return signature.substring(0, 15) + "..." + signature.substring(signature.length() - 5);
    }
    
    /**
     * Validate webhook IP address (optional extra security)
     * GitHub's IP ranges can be obtained from https://api.github.com/meta
     */
    public boolean validateSourceIP(String ipAddress, String[] allowedRanges) {
        // Implementation would check if IP is within GitHub's ranges
        // This is optional and requires maintaining an updated list of GitHub IPs
        logger.debug("IP validation for {} against allowed ranges", ipAddress);
        return true; // Simplified for now
    }
}