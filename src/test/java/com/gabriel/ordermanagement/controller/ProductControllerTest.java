package com.gabriel.ordermanagement.controller;

import com.gabriel.ordermanagement.dto.CreateProductRequest;
import com.gabriel.ordermanagement.dto.PageResponse;
import com.gabriel.ordermanagement.dto.ProductResponse;
import com.gabriel.ordermanagement.exception.ProductNotFoundException;
import com.gabriel.ordermanagement.service.ProductService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductControllerTest {

    private ProductService productService;
    private ProductController productController;

    @BeforeEach
    void setUp() {
        productService = mock(ProductService.class);
        productController = new ProductController(productService);
    }

    @Test
    void shouldReturnAllProducts() {
        ProductResponse product = new ProductResponse(
                1L,
                "Wireless Gaming Mouse",
                "Wireless gaming mouse",
                new BigDecimal("159.90"),
                OffsetDateTime.of(
                        2026, 10, 11, 12, 0, 0, 0,
                        ZoneOffset.UTC
                )
        );

        PageResponse<ProductResponse> pageResponse = new PageResponse<>(
                List.of(product),
                0,
                20,
                1,
                1,
                true,
                true
        );

        when(productService.findAll(
                0, 20, "id", Sort.Direction.DESC
        )).thenReturn(pageResponse);

        ResponseEntity<PageResponse<ProductResponse>> response =
                productController.findAll(
                        0, 20, "id", Sort.Direction.DESC
                );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        assertEquals(1, response.getBody().content().size());
        assertEquals(1, response.getBody().totalElements());
        assertEquals(1, response.getBody().totalPages());
        assertEquals(0, response.getBody().page());
        assertEquals(20, response.getBody().size());

        assertEquals(
                "Wireless Gaming Mouse",
                response.getBody().content().getFirst().name()
        );

        verify(productService).findAll(
                0, 20, "id", Sort.Direction.DESC
        );
    }

    @Test
    void shouldReturnProductById() {
        ProductResponse product = new ProductResponse(
                1L,
                "Wireless Gaming Mouse",
                "Wireless gaming mouse",
                new BigDecimal("159.90"),
                OffsetDateTime.of(
                        2026, 10, 11, 12, 0, 0, 0,
                        ZoneOffset.UTC
                )
        );

        when(productService.findById(1L)).thenReturn(product);

        ResponseEntity<ProductResponse> response =
                productController.findById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().id());
        assertEquals(
                "Wireless Gaming Mouse",
                response.getBody().name()
        );

        verify(productService).findById(1L);
    }

    @Test
    void shouldCreateProduct() {
        ProductResponse product = new ProductResponse(
                1L,
                "Wireless Gaming Mouse",
                "Wireless gaming mouse",
                new BigDecimal("159.90"),
                OffsetDateTime.of(
                        2026, 10, 11, 12, 0, 0, 0,
                        ZoneOffset.UTC
                )
        );

        CreateProductRequest request = new CreateProductRequest(
                "Wireless Gaming Mouse",
                "Wireless gaming mouse",
                new BigDecimal("159.90")
        );

        when(productService.create(
                request.name(),
                request.description(),
                request.price()
        )).thenReturn(product);

        ResponseEntity<ProductResponse> response =
                productController.create(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(
                "Wireless Gaming Mouse",
                response.getBody().name()
        );

        verify(productService).create(
                request.name(),
                request.description(),
                request.price()
        );
    }

    @Test
    void shouldUpdateProduct() {
        CreateProductRequest request = new CreateProductRequest(
                "Wireless Gaming Mouse Pro",
                "Professional wireless gaming mouse",
                new BigDecimal("199.90")
        );

        ProductResponse updatedProduct = new ProductResponse(
                1L,
                "Wireless Gaming Mouse Pro",
                "Professional wireless gaming mouse",
                new BigDecimal("199.90"),
                OffsetDateTime.of(
                        2026, 10, 11, 12, 0, 0, 0,
                        ZoneOffset.UTC
                )
        );

        when(productService.update(
                1L,
                request.name(),
                request.description(),
                request.price()
        )).thenReturn(updatedProduct);

        ResponseEntity<ProductResponse> response =
                productController.update(1L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(
                "Wireless Gaming Mouse Pro",
                response.getBody().name()
        );
        assertEquals(
                new BigDecimal("199.90"),
                response.getBody().price()
        );

        verify(productService).update(
                1L,
                request.name(),
                request.description(),
                request.price()
        );
    }

    @Test
    void shouldDeleteProduct() {
        ResponseEntity<Void> response =
                productController.delete(1L);

        assertEquals(
                HttpStatus.NO_CONTENT,
                response.getStatusCode()
        );
        assertNull(response.getBody());

        verify(productService).delete(1L);
    }

    @Test
    void shouldReturnEmptyListWhenNoProductsExist() {
        PageResponse<ProductResponse> emptyPage = new PageResponse<>(
                List.of(),
                0,
                20,
                0,
                0,
                true,
                true
        );

        when(productService.findAll(
                0, 20, "id", Sort.Direction.DESC
        )).thenReturn(emptyPage);

        ResponseEntity<PageResponse<ProductResponse>> response =
                productController.findAll(
                        0, 20, "id", Sort.Direction.DESC
                );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        assertTrue(response.getBody().content().isEmpty());
        assertEquals(0, response.getBody().totalElements());
        assertEquals(0, response.getBody().totalPages());
        assertTrue(response.getBody().first());
        assertTrue(response.getBody().last());

        verify(productService).findAll(
                0, 20, "id", Sort.Direction.DESC
        );
    }

    @Test
    void shouldPropagateExceptionWhenProductDoesNotExist() {
        when(productService.findById(999L))
                .thenThrow(new ProductNotFoundException(999L));

        assertThrows(
                ProductNotFoundException.class,
                () -> productController.findById(999L)
        );

        verify(productService).findById(999L);
    }
}