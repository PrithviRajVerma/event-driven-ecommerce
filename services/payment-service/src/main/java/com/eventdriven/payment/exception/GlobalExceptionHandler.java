package com.eventdriven.payment.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(PaymentServiceException.class)
    public ResponseEntity<ApiErrorResponse> handlePaymentServiceException(
            PaymentServiceException ex,
            HttpServletRequest request
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
                OffsetDateTime.now(),
                ex.getHttpStatus().value(),
                ex.getErrorCode(),
                ex.getMessage(),
                request.getRequestURI(),
                List.of()
        );
        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        List<ApiErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ApiErrorResponse.FieldError(
                        error.getField(),
                        error.getDefaultMessage()
                ))
                .toList();

        ApiErrorResponse response = new ApiErrorResponse(
                OffsetDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                "Request validation failed",
                request.getRequestURI(),
                fieldErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        List<ApiErrorResponse.FieldError> fieldErrors = ex.getConstraintViolations()
                .stream()
                .map(violation -> new ApiErrorResponse.FieldError(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()
                ))
                .toList();

        ApiErrorResponse response = new ApiErrorResponse(
                OffsetDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "CONSTRAINT_VIOLATION",
                "Constraint validation failed",
                request.getRequestURI(),
                fieldErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMalformedJson(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
                OffsetDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "MALFORMED_REQUEST_BODY",
                "Request body is malformed or unreadable",
                request.getRequestURI(),
                List.of()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthorizationDenied(
            AuthorizationDeniedException ex,
            HttpServletRequest request
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
                OffsetDateTime.now(),
                HttpStatus.FORBIDDEN.value(),
                "FORBIDDEN",
                "Access denied: insufficient permissions",
                request.getRequestURI(),
                List.of()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnhandledException(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Unhandled exception processing request [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);
        ApiErrorResponse response = new ApiErrorResponse(
                OffsetDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred",
                request.getRequestURI(),
                List.of()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
