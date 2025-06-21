package com.somdiproy.smartcode.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom Error Controller for handling application errors
 * Provides better error pages than the default whitelabel error page
 * 
 * @author Somdip Roy
 */
@Controller
public class CustomErrorController implements ErrorController {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomErrorController.class);
    
    /**
     * Handle HTML error responses
     */
    @RequestMapping(value = "/error", produces = MediaType.TEXT_HTML_VALUE)
    public String handleError(HttpServletRequest request, Model model) {
        logger.debug("Handling HTML error request");
        
        // Get error status code
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        
        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());
            model.addAttribute("status", statusCode);
            logger.debug("Error status code: {}", statusCode);
            
            // Add custom messages based on status code
            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                model.addAttribute("error", "Page Not Found");
                model.addAttribute("message", "The page you are looking for might have been removed, had its name changed, or is temporarily unavailable.");
            } else if (statusCode == HttpStatus.FORBIDDEN.value()) {
                model.addAttribute("error", "Access Denied");
                model.addAttribute("message", "You don't have permission to access this resource.");
            } else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                model.addAttribute("error", "Internal Server Error");
                model.addAttribute("message", "We're sorry, but something went wrong on our end. Please try again later.");
            } else if (statusCode == HttpStatus.BAD_REQUEST.value()) {
                model.addAttribute("error", "Bad Request");
                model.addAttribute("message", "The request could not be understood by the server due to malformed syntax.");
            } else if (statusCode == HttpStatus.UNAUTHORIZED.value()) {
                model.addAttribute("error", "Unauthorized");
                model.addAttribute("message", "You need to be authenticated to access this resource.");
            } else if (statusCode == HttpStatus.METHOD_NOT_ALLOWED.value()) {
                model.addAttribute("error", "Method Not Allowed");
                model.addAttribute("message", "The request method is not supported for this resource.");
            } else if (statusCode == HttpStatus.REQUEST_TIMEOUT.value()) {
                model.addAttribute("error", "Request Timeout");
                model.addAttribute("message", "The server timed out waiting for the request.");
            } else if (statusCode == HttpStatus.SERVICE_UNAVAILABLE.value()) {
                model.addAttribute("error", "Service Unavailable");
                model.addAttribute("message", "The server is temporarily unable to handle the request. Please try again later.");
            } else {
                model.addAttribute("error", "Error");
                model.addAttribute("message", "An unexpected error occurred while processing your request.");
            }
        } else {
            model.addAttribute("status", 500);
            model.addAttribute("error", "Unknown Error");
            model.addAttribute("message", "An unexpected error occurred.");
            logger.warn("No status code found in error attributes");
        }
        
        // Add additional error details if available
        Object errorMessage = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        if (errorMessage != null) {
            model.addAttribute("errorMessage", errorMessage.toString());
            logger.debug("Error message: {}", errorMessage);
        }
        
        Object errorException = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        if (errorException != null) {
            model.addAttribute("exception", errorException.toString());
            logger.error("Error exception: ", errorException);
        }
        
        String errorPath = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        if (errorPath != null) {
            model.addAttribute("path", errorPath);
            logger.debug("Error path: {}", errorPath);
        }
        
        // Add timestamp
        model.addAttribute("timestamp", new Date());
        
        // Log the error for monitoring
        logger.info("Returning error view for status: {} at path: {}", 
                   model.getAttribute("status"), 
                   model.getAttribute("path"));
        
        return "error";
    }
    
    /**
     * Handle API error responses with JSON
     */
    @RequestMapping(value = "/error", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> handleErrorApi(HttpServletRequest request) {
        logger.debug("Handling API error request");
        
        Map<String, Object> errorAttributes = new HashMap<>();
        
        // Get status code
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int statusCode = 500;
        if (status != null) {
            statusCode = Integer.parseInt(status.toString());
        }
        errorAttributes.put("status", statusCode);
        
        // Get error message
        Object error = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        if (error != null) {
            errorAttributes.put("error", error.toString());
        } else {
            errorAttributes.put("error", HttpStatus.valueOf(statusCode).getReasonPhrase());
        }
        
        // Add custom message based on status
        String message = getApiErrorMessage(statusCode);
        errorAttributes.put("message", message);
        
        // Add additional details
        errorAttributes.put("timestamp", new Date());
        errorAttributes.put("path", request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI));
        
        // Log API error
        logger.warn("API error response: status={}, path={}", statusCode, errorAttributes.get("path"));
        
        return errorAttributes;
    }
    
    /**
     * Get appropriate error message for API responses
     */
    private String getApiErrorMessage(int statusCode) {
        switch (statusCode) {
            case 400:
                return "Invalid request parameters";
            case 401:
                return "Authentication required";
            case 403:
                return "Access forbidden";
            case 404:
                return "Resource not found";
            case 405:
                return "Method not allowed";
            case 408:
                return "Request timeout";
            case 429:
                return "Too many requests";
            case 500:
                return "Internal server error";
            case 502:
                return "Bad gateway";
            case 503:
                return "Service unavailable";
            case 504:
                return "Gateway timeout";
            default:
                return "An error occurred processing your request";
        }
    }
}