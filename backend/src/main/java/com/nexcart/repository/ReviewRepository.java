package com.nexcart.repository;

import com.nexcart.entity.Product;
import com.nexcart.entity.Review;
import com.nexcart.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    @EntityGraph(attributePaths = {
            "user",
            "product"
    })
    Page<Review> findByProduct(Product product, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {
            "user",
            "product"
    })
    Optional<Review> findById(Long id);

    @EntityGraph(attributePaths = {
            "user",
            "product"
    })
    Page<Review> findAll(Pageable pageable);

    Optional<Review> findByUserAndProduct(User user, Product product);

    boolean existsByUserAndProduct(User user, Product product);
}
