package com.nexcart.repository;

import com.nexcart.entity.Product;
import com.nexcart.entity.Review;
import com.nexcart.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    @EntityGraph(attributePaths = {
            "user",
            "product"
    })
    List<Review> findByProduct(Product product);

    @Override
    @EntityGraph(attributePaths = {
            "user",
            "product"
    })
    Optional<Review> findById(Long id);

    Optional<Review> findByUserAndProduct(User user, Product product);

    boolean existsByUserAndProduct(User user, Product product);

}
