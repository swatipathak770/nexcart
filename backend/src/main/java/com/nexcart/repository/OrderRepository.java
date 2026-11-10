package com.nexcart.repository;

import com.nexcart.entity.Order;
import com.nexcart.entity.OrderStatus;
import com.nexcart.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByUser(User user, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {
            "orderItems",
            "orderItems.product",
            "user",
            "coupon"
    })
    Optional<Order> findById(Long id);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
}
