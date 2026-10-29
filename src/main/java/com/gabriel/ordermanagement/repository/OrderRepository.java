package com.gabriel.ordermanagement.repository;

import com.gabriel.ordermanagement.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query(
            value = "SELECT o.id FROM Order o ORDER BY o.id DESC",
            countQuery = "SELECT COUNT(o) FROM Order o"
    )
    Page<Long> findPageIds(Pageable pageable);

    @Query("""
        SELECT DISTINCT o FROM Order o
        LEFT JOIN FETCH o.orderItems oi
        LEFT JOIN FETCH oi.product
        WHERE o.id IN :ids
        """)
    List<Order> findAllWithItemsByIdIn(
            @Param("ids") List<Long> ids
    );

    @Query("""
        SELECT DISTINCT o FROM Order o
        LEFT JOIN FETCH o.orderItems oi
        LEFT JOIN FETCH oi.product
        WHERE o.id = :id
        """)
    Optional<Order> findWithItemsById(@Param("id") Long id);
}