package com.travelo.postservice.exception;

import com.travelo.postservice.dto.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(PostNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handlePostNotFound(PostNotFoundException ex) {
        logger.warn("Post not found: {}", ex.getMessage());
        ApiResponse<?> response = ApiResponse.error(
                ex.getMessage(),
                "POST_NOT_FOUND"
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<?>> handleUnauthorized(UnauthorizedException ex) {
        logger.warn("Unauthorized access: {}", ex.getMessage());
        ApiResponse<?> response = ApiResponse.error(
                ex.getMessage(),
                "FORBIDDEN"
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiResponse<?>> handleSecurityException(SecurityException ex) {
        logger.warn("Security violation: {}", ex.getMessage());
        ApiResponse<?> response = ApiResponse.error(
                ex.getMessage(),
                "FORBIDDEN"
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidationErrors(MethodArgumentNotValidException ex) {
        logger.warn("Validation error: {}", ex.getMessage());
        Map<String, Object> details = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
                details.put(error.getField(), error.getDefaultMessage())
        );
        logger.debug("Validation error details: {}", details);
        
        ApiResponse<?> response = ApiResponse.error(
                "Validation failed",
                "VALIDATION_ERROR",
                details
        );
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<?>> handleConstraintViolation(ConstraintViolationException ex) {
        logger.warn("Constraint violation: {}", ex.getMessage());
        Map<String, Object> details = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> 
                details.put(violation.getPropertyPath().toString(), violation.getMessage())
        );
        logger.debug("Constraint violation details: {}", details);
        
        ApiResponse<?> response = ApiResponse.error(
                "Constraint violation",
                "VALIDATION_ERROR",
                details
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<?>> handleDataIntegrity(DataIntegrityViolationException ex) {
        String detail = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
        logger.error("Data integrity violation: {}", detail, ex);
        String userMessage = "Data integrity violation";
        if (detail != null && !detail.isBlank()) {
            if (detail.contains("unique") || detail.contains("duplicate")) {
                userMessage = "Duplicate or conflicting data. Please try again with different content.";
            } else if (detail.length() <= 200) {
                userMessage = detail;
            }
        }
        ApiResponse<?> response = ApiResponse.error(
                userMessage,
                "CONFLICT"
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<?>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        String message = ex.getMessage();
        String detailedMessage = "Invalid request body format";
        Map<String, Object> details = new HashMap<>();
        
        // Provide more specific error messages for common issues
        if (message != null) {
            if (message.contains("UUID") && (message.contains("String") || message.contains("email"))) {
                detailedMessage = "Invalid UUID format. UUID fields must be in standard 36-character format (e.g., '550e8400-e29b-41d4-a716-446655440000'). " +
                                 "If you're sending an email address or other identifier, please use the user's UUID instead.";
                details.put("hint", "The 'owner_id' field expects a UUID, not an email address. Please retrieve the user's UUID from the authentication service first.");
            } else if (message.contains("Cannot deserialize")) {
                detailedMessage = "Invalid data type in request body. " + 
                                 (message.length() > 200 ? message.substring(0, 200) + "..." : message);
            }
            details.put("originalError", message);
        }
        
        logger.warn("Invalid request body: {}", message);
        ApiResponse<?> response = ApiResponse.error(
                detailedMessage,
                "INVALID_REQUEST_BODY",
                details
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGeneric(Exception ex) {
        logger.error("Unexpected error occurred", ex);
        ApiResponse<?> response = ApiResponse.error(
                ex.getMessage(),
                "INTERNAL_SERVER_ERROR"
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}

