package com.gabriel.ordermanagement.repository;

import com.gabriel.ordermanagement.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}