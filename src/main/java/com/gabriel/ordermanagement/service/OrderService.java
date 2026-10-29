package com.gabriel.ordermanagement.service;

import com.gabriel.ordermanagement.dto.CreateOrderItemRequest;
import com.gabriel.ordermanagement.dto.CreateOrderRequest;
import com.gabriel.ordermanagement.dto.OrderResponse;
import com.gabriel.ordermanagement.dto.PageResponse;
import com.gabriel.ordermanagement.entity.Order;
import com.gabriel.ordermanagement.entity.OrderItem;
import com.gabriel.ordermanagement.entity.Product;
import com.gabriel.ordermanagement.enums.OrderStatus;
import com.gabriel.ordermanagement.exception.OrderNotFoundException;
import com.gabriel.ordermanagement.exception.ProductNotFoundException;
import com.gabriel.ordermanagement.repository.OrderRepository;
import com.gabriel.ordermanagement.repository.ProductRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    @Transactional(readOnly = true)
    public OrderResponse findById(Long id) {
        Order order = orderRepository.findWithItemsById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> findAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Page<Long> idPage = orderRepository.findPageIds(pageable);

        if (idPage.isEmpty()) {
            return PageResponse.from(
                    idPage.map(id -> (OrderResponse) null)
            );
        }

        List<Order> orders = orderRepository
                .findAllWithItemsByIdIn(idPage.getContent());

        Map<Long, Order> ordersById = orders.stream()
                .collect(Collectors.toMap(
                        Order::getId,
                        Function.identity()
                ));

        List<OrderResponse> responses = idPage.getContent()
                .stream()
                .map(ordersById::get)
                .map(OrderResponse::from)
                .toList();

        Page<OrderResponse> result = new PageImpl<>(
                responses,
                pageable,
                idPage.getTotalElements()
        );

        return PageResponse.from(result);
    }
}