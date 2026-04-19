package com.travelo.mediaservice.exception;

import com.travelo.mediaservice.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MediaFileNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMediaNotFound(MediaFileNotFoundException ex, HttpServletRequest request) {
        log.debug("Media not found: {} path={}", ex.getMessage(), request.getRequestURI());
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Bad request path={} msg={}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(SecurityException ex, HttpServletRequest request) {
        log.warn("Forbidden path={} msg={}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), request, null);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUpload(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        log.warn("Multipart too large path={}", request.getRequestURI(), ex);
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "Uploaded file exceeds configured max size", request, null);
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ErrorResponse> handleMultipart(MultipartException ex, HttpServletRequest request) {
        log.warn("Multipart error path={} msg={}", request.getRequestURI(), ex.getMessage(), ex);
        return build(HttpStatus.BAD_REQUEST, "Multipart request error: " + ex.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ErrorResponse.ValidationError> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::mapFieldError)
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Validation failed", request, errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(ConstraintViolationException ex, HttpServletRequest request) {
        List<ErrorResponse.ValidationError> errors = ex.getConstraintViolations()
                .stream()
                .map(violation -> new ErrorResponse.ValidationError(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()
                ))
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Constraint violation", request, errors);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "Data integrity violation", request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception method={} path={} msg={}",
                request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);
        String clientMessage = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
        return build(HttpStatus.INTERNAL_SERVER_ERROR, clientMessage, request, null);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status,
                                                String message,
                                                HttpServletRequest request,
                                                List<ErrorResponse.ValidationError> errors) {
        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                (errors == null || errors.isEmpty()) ? null : errors
        );
        return ResponseEntity.status(status).body(body);
    }

    private ErrorResponse.ValidationError mapFieldError(FieldError fieldError) {
        return new ErrorResponse.ValidationError(fieldError.getField(), fieldError.getDefaultMessage());
    }
}

