
package com.gabriel.ordermanagement.controller;

import com.gabriel.ordermanagement.dto.CreateProductRequest;
import com.gabriel.ordermanagement.dto.ProductResponse;
import com.gabriel.ordermanagement.exception.ProductNotFoundException;
import com.gabriel.ordermanagement.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductControllerTest {

    @Test
    void shouldReturnAllProducts() {
        ProductService productService = mock(ProductService.class);
        ProductController productController =
                new ProductController(productService);

        ProductResponse product = new ProductResponse(
                1L,
                "Wireless Gaming Mouse",
                "Wireless gaming mouse",
                new BigDecimal("159.90"),
                OffsetDateTime.of(
                        2026, 10, 11, 12, 0, 0, 0,
                        ZoneOffset.UTC)
        );

        when(productService.findAll())
                .thenReturn(List.of(product));

        ResponseEntity<List<ProductResponse>> response = productController.findAll();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("Wireless Gaming Mouse",
                response.getBody().getFirst().name());
    }

    @Test
    void shouldReturnProductById() {
        ProductService productService = mock(ProductService.class);
        ProductController productController = new ProductController(productService);

        ProductResponse product = new ProductResponse(
                1L,
                "Wireless Gaming Mouse",
                "Wireless gaming mouse",
                new BigDecimal("159.90"),
                OffsetDateTime.of(2026, 10, 11, 12, 0, 0, 0, ZoneOffset.UTC)
        );

        when(productService.findById(1L)).thenReturn(product);

        ResponseEntity<ProductResponse> response = productController.findById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().id());
        assertEquals("Wireless Gaming Mouse", response.getBody().name());
    }


    @Test
    void shouldCreateProduct() {
        ProductService productService = mock(ProductService.class);
        ProductController productController =
                new ProductController(productService);

        ProductResponse product = new ProductResponse(
                1L,
                "Wireless Gaming Mouse",
                "Wireless gaming mouse",
                new BigDecimal("159.90"),
                OffsetDateTime.of(2026, 10, 11, 12, 0, 0, 0, ZoneOffset.UTC)
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
        assertEquals("Wireless Gaming Mouse", response.getBody().name());

        verify(productService).create(request.name(), request.description(), request.price());
    }

    @Test
    void shouldUpdateProduct() {
        ProductService productService = mock(ProductService.class);
        ProductController productController = new ProductController(productService);

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
                OffsetDateTime.of(2026, 10, 11, 12, 0, 0, 0, ZoneOffset.UTC)
        );

        when(productService.update(1L, request.name(), request.description(), request.price())).thenReturn(updatedProduct);

        ResponseEntity<ProductResponse> response = productController.update(1L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Wireless Gaming Mouse Pro", response.getBody().name());
        assertEquals(new BigDecimal("199.90"), response.getBody().price());
        verify(productService).update(1L, request.name(), request.description(), request.price());
    }

    @Test
    void shouldDeleteProduct() {
        ProductService productService = mock(ProductService.class);
        ProductController productController = new ProductController(productService);

        ResponseEntity<Void> response = productController.delete(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());

        verify(productService).delete(1L);
    }

    @Test
    void shouldReturnEmptyListWhenNoProductsExist() {
        ProductService productService = mock(ProductService.class);
        ProductController productController = new ProductController(productService);

        when(productService.findAll()).thenReturn(List.of());

        ResponseEntity<List<ProductResponse>> response = productController.findAll();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().size());

        verify(productService).findAll();
    }

    @Test
    void shouldPropagateExceptionWhenProductDoesNotExist() {
        ProductService productService = mock(ProductService.class);
        ProductController productController = new ProductController(productService);

        when(productService.findById(999L)).thenThrow(new ProductNotFoundException(999L));

        assertThrows(ProductNotFoundException.class, () -> productController.findById(999L));

        verify(productService).findById(999L);
    }
}