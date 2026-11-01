package com.gabriel.ordermanagement.controller;

import com.gabriel.ordermanagement.exception.GlobalExceptionHandler;
import com.gabriel.ordermanagement.exception.ProductNotFoundException;
import com.gabriel.ordermanagement.service.ProductService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProductControllerErrorTest {

    private ProductService productService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        productService = mock(ProductService.class);

        LocalValidatorFactoryBean validator =
                new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new ProductController(productService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void shouldReturnNotFoundForNonexistentProduct()
            throws Exception {

        when(productService.findById(999L))
                .thenThrow(new ProductNotFoundException(999L));

        mockMvc.perform(get("/api/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message")
                        .value("Product not found with id: 999"))
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());

        verify(productService).findById(999L);
    }

    @Test
    void shouldRejectProductWithMissingName() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "description": "Test product",
                                  "price": 99.90
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message")
                        .value("Request validation failed"))
                .andExpect(jsonPath("$.fieldErrors.name")
                        .value("Product name is required"));

        verifyNoInteractions(productService);
    }

    @Test
    void shouldRejectNegativeProductPrice() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Keyboard",
                                  "description": "Test product",
                                  "price": -10.00
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.price")
                        .value("Product price must not be negative"));

        verifyNoInteractions(productService);
    }

    @Test
    void shouldRejectInvalidPagination() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("page", "-1")
                        .param("size", "20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Invalid pagination parameters"));

        verifyNoInteractions(productService);
    }

    @Test
    void shouldRejectInvalidSortField() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("sortBy", "invalidField"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Invalid sorting field"));

        verifyNoInteractions(productService);
    }

    @Test
    void shouldRejectMalformedProductJson() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Keyboard",
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Malformed or invalid JSON request body"));

        verifyNoInteractions(productService);
    }
}