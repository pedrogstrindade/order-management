package com.gabriel.ordermanagement.service;

import com.gabriel.ordermanagement.dto.CreateOrderItemRequest;
import com.gabriel.ordermanagement.dto.CreateOrderRequest;
import com.gabriel.ordermanagement.dto.OrderResponse;
import com.gabriel.ordermanagement.entity.Order;
import com.gabriel.ordermanagement.entity.OrderItem;
import com.gabriel.ordermanagement.entity.Product;
import com.gabriel.ordermanagement.enums.OrderStatus;
import com.gabriel.ordermanagement.exception.ProductNotFoundException;
import com.gabriel.ordermanagement.repository.OrderRepository;
import com.gabriel.ordermanagement.repository.ProductRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public OrderService(
            OrderRepository orderRepository,
            ProductRepository productRepository
    ) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest request) {

        Order order = new Order(OrderStatus.PENDING);

        for (CreateOrderItemRequest itemRequest : request.items()) {

            Product product = productRepository
                    .findById(itemRequest.productId())
                    .orElseThrow(() ->
                            new ProductNotFoundException(
                                    itemRequest.productId()
                            )
                    );

            OrderItem orderItem = new OrderItem(
                    product.getPrice(),
                    product,
                    order,
                    itemRequest.quantity()
            );

            order.addOrderItem(orderItem);
        }

        Order savedOrder = orderRepository.save(order);

        return OrderResponse.from(savedOrder);
    }
}