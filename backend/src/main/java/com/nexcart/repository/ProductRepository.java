package com.nexcart.repository;

import com.nexcart.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends
        JpaRepository<Product, Long>,
        JpaSpecificationExecutor<Product> {

    boolean existsBySku(String sku);

    Optional<Product> findBySku(String sku);

    @Override
    @EntityGraph(attributePaths = {"category", "brand"})
    List<Product> findAll();

    @Override
    @EntityGraph(attributePaths = {"category", "brand"})
    Page<Product> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"category", "brand"})
    Optional<Product> findById(Long id);

    @EntityGraph(attributePaths = {"category", "brand"})
    List<Product> findByNameContainingIgnoreCase(String keyword);
}
