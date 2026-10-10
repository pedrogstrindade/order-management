package com.gabriel.ordermanagement.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 150)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public Product() {}

    public Product(String name, String description, BigDecimal price) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.createdAt = OffsetDateTime.now();
    }
}