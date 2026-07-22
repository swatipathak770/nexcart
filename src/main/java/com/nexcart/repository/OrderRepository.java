package com.nexcart.repository;

import com.nexcart.entity.Order;
import com.nexcart.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {
            "orderItems",
            "orderItems.product",
            "user"
    })
    List<Order> findByUser(User user);

    @Override
    @EntityGraph(attributePaths = {
            "orderItems",
            "orderItems.product",
            "user"
    })
    java.util.Optional<Order> findById(Long id);
}
