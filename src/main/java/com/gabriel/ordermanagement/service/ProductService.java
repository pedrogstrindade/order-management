package com.gabriel.ordermanagement.service;

import com.gabriel.ordermanagement.dto.PageResponse;
import com.gabriel.ordermanagement.dto.ProductResponse;
import com.gabriel.ordermanagement.entity.Product;
import com.gabriel.ordermanagement.exception.ProductNotFoundException;
import com.gabriel.ordermanagement.repository.ProductRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> findAll(
            int page,
            int size,
            String sortBy,
            Sort.Direction direction
    ) {
        Sort sort = Sort.by(direction, sortBy);

        if (!sortBy.equals("id")) {
            sort = sort.and(Sort.by(Sort.Direction.ASC, "id"));
        }

        Pageable pageable = PageRequest.of(page, size, sort);

        return PageResponse.from(
                productRepository.findAll(pageable)
                        .map(ProductResponse::from)
        );
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse create(
            String name,
            String description,
            BigDecimal price
    ) {
        Product product = new Product(name, description, price);
        Product savedProduct = productRepository.save(product);

        return ProductResponse.from(savedProduct);
    }

    @Transactional
    public ProductResponse update(
            Long id,
            String name,
            String description,
            BigDecimal price
    ) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);

        Product savedProduct = productRepository.save(product);

        return ProductResponse.from(savedProduct);
    }

    @Transactional
    public void delete(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        productRepository.delete(product);
    }
}