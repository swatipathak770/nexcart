package com.nexcart.repository;

import com.nexcart.entity.Cart;
import com.nexcart.entity.Product;
import com.nexcart.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    @EntityGraph(attributePaths = {"product", "product.category", "product.brand"})
    List<Cart> findByUser(User user);

    Optional<Cart> findByUserAndProduct(User user, Product product);

    void deleteByUser(User user);
}
