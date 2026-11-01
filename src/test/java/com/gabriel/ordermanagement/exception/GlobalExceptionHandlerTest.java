package com.gabriel.ordermanagement.exception;

import com.gabriel.ordermanagement.dto.ApiErrorResponse;

import jakarta.persistence.OptimisticLockException;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler =
            new GlobalExceptionHandler();

    @Test
    void shouldReturnConflictForOptimisticLockException() {
        OptimisticLockException exception =
                new OptimisticLockException(
                        "Row was updated by another transaction"
                );

        ResponseEntity<ApiErrorResponse> response =
                handler.handleOptimisticLockConflict(exception);

        assertEquals(
                HttpStatus.CONFLICT,
                response.getStatusCode()
        );

        ApiErrorResponse body = response.getBody();

        assertNotNull(body);
        assertEquals(409, body.status());
        assertEquals("Conflict", body.error());
        assertEquals(
                "Order was modified concurrently. Please reload and try again.",
                body.message()
        );
        assertTrue(body.fieldErrors().isEmpty());
    }

    @Test
    void shouldReturnNotFoundForProduct() {
        ResponseEntity<ApiErrorResponse> response =
                handler.handleProductNotFound(
                        new ProductNotFoundException(999L)
                );

        assertEquals(
                HttpStatus.NOT_FOUND,
                response.getStatusCode()
        );

        ApiErrorResponse body = response.getBody();

        assertNotNull(body);
        assertEquals(404, body.status());
        assertEquals("Not Found", body.error());
        assertEquals(
                "Product not found with id: 999",
                body.message()
        );
    }

    @Test
    void shouldReturnNotFoundForOrder() {
        ResponseEntity<ApiErrorResponse> response =
                handler.handleOrderNotFound(
                        new OrderNotFoundException(999L)
                );

        assertEquals(
                HttpStatus.NOT_FOUND,
                response.getStatusCode()
        );

        ApiErrorResponse body = response.getBody();

        assertNotNull(body);
        assertEquals(404, body.status());
        assertEquals("Not Found", body.error());
        assertEquals(
                "Order not found with ID: 999",
                body.message()
        );
    }
}