package com.gabriel.ordermanagement.service;

import com.gabriel.ordermanagement.dto.ProductResponse;
import com.gabriel.ordermanagement.entity.Product;
import com.gabriel.ordermanagement.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ProductResponse> findAll() {
        return productRepository.findAll()
                .stream()
                .map(ProductResponse::from)
                .toList();
    }

    public ProductResponse create(String name, String description, BigDecimal price) {
        Product product = new Product(name, description, price);
        Product savedProduct = productRepository.save(product);

        return ProductResponse.from(savedProduct);
    }

    public ProductResponse findById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Product not found"));

        return ProductResponse.from(product);
    }


    public ProductResponse update(Long id, String name, String description, BigDecimal price) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product not found"
                ));

        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);

        Product updatedProduct = productRepository.save(product);

        return ProductResponse.from(updatedProduct);
    }

    public void delete(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product not found"
                ));

        productRepository.delete(product);
    }
}