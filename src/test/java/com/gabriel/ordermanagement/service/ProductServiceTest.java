package com.gabriel.ordermanagement.service;

import com.gabriel.ordermanagement.dto.ProductResponse;
import com.gabriel.ordermanagement.entity.Product;
import com.gabriel.ordermanagement.exception.ProductNotFoundException;
import com.gabriel.ordermanagement.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void shouldCreateProduct() {
        BigDecimal price = new BigDecimal("159.90");

        Product product = new Product(
                "Wireless Gaming Mouse",
                "Wireless gaming mouse with six programmable buttons",
                price
        );

        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductResponse response = productService.create(
                product.getName(),
                product.getDescription(),
                product.getPrice()
        );

        assertEquals("Wireless Gaming Mouse", response.name());
        assertEquals(price, response.price());

        verify(productRepository).save(any(Product.class));
    }

    @Test
    void shouldFindProductById() {
        Long id = 1L;
        BigDecimal price = new BigDecimal("159.90");

        Product product = new Product(
                "Wireless Gaming Mouse",
                "Wireless gaming mouse with six programmable buttons",
                price
        );

        when(productRepository.findById(id)).thenReturn(Optional.of(product));

        ProductResponse response = productService.findById(id);

        assertEquals("Wireless Gaming Mouse", response.name());
        assertEquals(price, response.price());

        verify(productRepository).findById(id);
    }

    @Test
    void shouldThrowExceptionWhenProductDoesNotExist() {
        Long id = 999L;

        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.findById(id));

        verify(productRepository).findById(id);
    }

    @Test
    void shouldUpdateProduct() {
        Long id = 1L;
        BigDecimal oldPrice = new BigDecimal("159.90");
        BigDecimal newPrice = new BigDecimal("199.90");

        Product existingProduct = new Product(
                "Wireless Gaming Mouse",
                "Wireless gaming mouse",
                oldPrice
        );

        when(productRepository.findById(id)).thenReturn(Optional.of(existingProduct));

        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response = productService.update(
                id,
                "Gaming Mouse Pro",
                "Wireless gaming mouse with rgb lighting",
                newPrice
        );

        assertEquals("Gaming Mouse Pro", response.name());
        assertEquals("Wireless gaming mouse with rgb lighting", response.description());
        assertEquals(newPrice, response.price());

        verify(productRepository).findById(id);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void shouldDeleteProduct() {
        Long id = 1L;

        Product product = new Product(
                "Wireless Gaming Mouse",
                "Wireless gaming mouse",
                new BigDecimal("159.90")
        );

        when(productRepository.findById(id)).thenReturn(Optional.of(product));

        productService.delete(id);

        verify(productRepository).findById(id);
        verify(productRepository).delete(product);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonexistentProduct() {
        Long id = 999L;

        when(productRepository.findById(id))
                .thenReturn(java.util.Optional.empty());

        assertThrows(
                ProductNotFoundException.class,
                () -> productService.delete(id)
        );

        verify(productRepository).findById(id);
        verify(productRepository, never()).delete(any(Product.class));
    }
}