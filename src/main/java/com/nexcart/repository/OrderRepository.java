package com.nexcart.repository;

import com.nexcart.entity.Order;
import com.nexcart.entity.OrderStatus;
import com.nexcart.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {
            "orderItems",
            "orderItems.product",
            "user",
            "coupon"
    })
    List<Order> findByUser(User user);

    @Override
    @EntityGraph(attributePaths = {
            "orderItems",
            "orderItems.product",
            "user",
            "coupon"
    })
    Optional<Order> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {
            "orderItems",
            "orderItems.product",
            "user",
            "coupon"
    })
    List<Order> findAll();

    @EntityGraph(attributePaths = {
            "orderItems",
            "orderItems.product",
            "user",
            "coupon"
    })
    List<Order> findByStatus(OrderStatus status);
}