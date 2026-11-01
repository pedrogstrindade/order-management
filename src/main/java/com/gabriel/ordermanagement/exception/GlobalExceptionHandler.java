package com.gabriel.ordermanagement.exception;

import com.gabriel.ordermanagement.dto.ApiErrorResponse;
import jakarta.persistence.OptimisticLockException;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleProductNotFound(
            ProductNotFoundException exception
    ) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleOrderNotFound(
            OrderNotFoundException exception
    ) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(InvalidOrderStatusTransitionException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidOrderStatusTransition(
            InvalidOrderStatusTransitionException exception
    ) {
        return error(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler({
            OptimisticLockingFailureException.class,
            OptimisticLockException.class
    })
    public ResponseEntity<ApiErrorResponse> handleOptimisticLockConflict(
            Exception exception
    ) {
        return error(
                HttpStatus.CONFLICT,
                "Order was modified concurrently. Please reload and try again."
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException exception
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        exception.getBindingResult()
                .getFieldErrors()
                .forEach(fieldError ->
                        fieldErrors.putIfAbsent(
                                fieldError.getField(),
                                fieldError.getDefaultMessage() != null
                                        ? fieldError.getDefaultMessage()
                                        : "Invalid value"
                        )
                );

        ApiErrorResponse body = new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Request validation failed",
                fieldErrors
        );

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableMessage(
            HttpMessageNotReadableException exception
    ) {
        return error(
                HttpStatus.BAD_REQUEST,
                "Malformed or invalid JSON request body"
        );
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatusException(
            ResponseStatusException exception
    ) {
        HttpStatusCode statusCode = exception.getStatusCode();

        String error = statusCode.is4xxClientError()
                ? "Client Error"
                : "Server Error";

        HttpStatus resolvedStatus =
                HttpStatus.resolve(statusCode.value());

        if (resolvedStatus != null) {
            error = resolvedStatus.getReasonPhrase();
        }

        ApiErrorResponse body = new ApiErrorResponse(
                statusCode.value(),
                error,
                exception.getReason() != null
                        ? exception.getReason()
                        : error
        );

        return ResponseEntity.status(statusCode).body(body);
    }

    private ResponseEntity<ApiErrorResponse> error(
            HttpStatus status,
            String message
    ) {
        ApiErrorResponse body = new ApiErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                message
        );

        return ResponseEntity.status(status).body(body);
    }
}