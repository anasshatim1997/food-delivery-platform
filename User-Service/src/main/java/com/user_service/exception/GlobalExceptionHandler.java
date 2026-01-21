package com.user_service.exception;

import com.user_service.dto.response.ApiResponse;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.Nonnull;
import jakarta.validation.ConstraintViolationException;
import lombok.NonNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFoundException(
            ResourceNotFoundException ex) {
        ApiResponse<Object> response = ApiResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .message(ex.getMessage())
                .data(null)
                .build();
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Object>> handleDuplicateResourceException(
            DuplicateResourceException ex) {
        ApiResponse<Object> response = ApiResponse.builder()
                .status(HttpStatus.CONFLICT.value())
                .message(ex.getMessage())
                .data(null)
                .build();
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(InvalidOperationException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidOperationException(
            InvalidOperationException ex) {
        ApiResponse<Object> response = ApiResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message(ex.getMessage())
                .data(null)
                .build();
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ApiResponse<Map<String, String>> response = ApiResponse.<Map<String, String>>builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message("Validation failed")
                .data(errors)
                .build();
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleConstraintViolationException(
            ConstraintViolationException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String propertyPath = violation.getPropertyPath().toString();
            String message = violation.getMessage();
            errors.put(propertyPath, message);
        });

        ApiResponse<Map<String, String>> response = ApiResponse.<Map<String, String>>builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message("Validation constraint violation")
                .data(errors)
                .build();
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex) {
        String message = "Data integrity violation";
        String exceptionMessage = ex.getMessage();
        if (exceptionMessage != null) {
            if (exceptionMessage.contains("email")) {
                message = "Email already exists";
            } else if (exceptionMessage.contains("phone")) {
                message = "Phone number already exists";
            }
        }

        ApiResponse<Object> response = ApiResponse.<Object>builder()
                .status(HttpStatus.CONFLICT.value())
                .message(message)
                .data(null)
                .build();
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadCredentialsException(
            BadCredentialsException ex) {
        ApiResponse<Object> response = ApiResponse.<Object>builder()
                .status(HttpStatus.UNAUTHORIZED.value())
                .message("Invalid email or password")
                .data(null)
                .build();
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<Object>> handleDisabledException(
            DisabledException ex) {
        ApiResponse<Object> response = ApiResponse.<Object>builder()
                .status(HttpStatus.UNAUTHORIZED.value())
                .message("Account is disabled or suspended")
                .data(null)
                .build();
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDeniedException(
            AccessDeniedException ex) {
        ApiResponse<Object> response = ApiResponse.<Object>builder()
                .status(HttpStatus.FORBIDDEN.value())
                .message("Access denied. You don't have permission to access this resource")
                .data(null)
                .build();
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Object>> handleAuthenticationException(
            AuthenticationException ex) {
        ApiResponse<Object> response = ApiResponse.<Object>builder()
                .status(HttpStatus.UNAUTHORIZED.value())
                .message("Authentication failed: " + ex.getMessage())
                .data(null)
                .build();
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ApiResponse<Object>> handleExpiredJwtException(
            ExpiredJwtException ex) {
        ApiResponse<Object> response = ApiResponse.<Object>builder()
                .status(HttpStatus.UNAUTHORIZED.value())
                .message("JWT token has expired")
                .data(null)
                .build();
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(MalformedJwtException.class)
    public ResponseEntity<ApiResponse<Object>> handleMalformedJwtException(
            MalformedJwtException ex) {
        ApiResponse<Object> response = ApiResponse.<Object>builder()
                .status(HttpStatus.UNAUTHORIZED.value())
                .message("Invalid JWT token format")
                .data(null)
                .build();
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(SignatureException.class)
    public ResponseEntity<ApiResponse<Object>> handleSignatureException(
            SignatureException ex) {
        ApiResponse<Object> response = ApiResponse.<Object>builder()
                .status(HttpStatus.UNAUTHORIZED.value())
                .message("Invalid JWT signature")
                .data(null)
                .build();
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Object>> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex) {
        ApiResponse<Object> response = ApiResponse.<Object>builder()
                .status(HttpStatus.PAYLOAD_TOO_LARGE.value())
                .message("File size exceeds maximum allowed size: " + ex.getMessage())
                .data(null)
                .build();
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgumentException(
            IllegalArgumentException ex) {
        ApiResponse<Object> response = ApiResponse.<Object>builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message(ex.getMessage())
                .data(null)
                .build();
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<@NonNull  ApiResponse<Object>> handleGlobalException(
            Exception ex) {
        ApiResponse<Object> response = ApiResponse.<Object>builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message("An unexpected error occurred: " + ex.getMessage())
                .data(null)
                .build();
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}