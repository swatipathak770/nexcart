package com.nexcart.repository;

import com.nexcart.entity.Product;
import com.nexcart.entity.User;
import com.nexcart.entity.Wishlist;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    @EntityGraph(attributePaths = {
            "product",
            "product.category",
            "product.brand"
    })
    List<Wishlist> findByUser(User user);

    @Override
    @EntityGraph(attributePaths = {
            "user",
            "product"
    })
    Optional<Wishlist> findById(Long id);

    Optional<Wishlist> findByUserAndProduct(User user, Product product);

    boolean existsByUserAndProduct(User user, Product product);
}
