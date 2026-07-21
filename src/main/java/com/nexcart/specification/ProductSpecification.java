package com.nexcart.specification;

import com.nexcart.entity.Product;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public class ProductSpecification {

    public static Specification<Product> hasKeyword(String keyword) {
        return (root, query, cb) -> {

            if (keyword == null || keyword.isBlank()) {
                return cb.conjunction();
            }

            return cb.like(
                    cb.lower(root.get("name")),
                    "%" + keyword.toLowerCase() + "%"
            );
        };
    }

    public static Specification<Product> hasCategory(String category) {

        return (root, query, cb) -> {

            if (category == null || category.isBlank()) {
                return cb.conjunction();
            }

            return cb.equal(
                    cb.lower(root.get("category").get("name")),
                    category.toLowerCase()
            );
        };
    }

    public static Specification<Product> hasBrand(String brand) {

        return (root, query, cb) -> {

            if (brand == null || brand.isBlank()) {
                return cb.conjunction();
            }

            return cb.equal(
                    cb.lower(root.get("brand").get("name")),
                    brand.toLowerCase()
            );
        };
    }

    public static Specification<Product> hasMinPrice(BigDecimal minPrice) {

        return (root, query, cb) -> {

            if (minPrice == null) {
                return cb.conjunction();
            }

            return cb.greaterThanOrEqualTo(
                    root.get("price"),
                    minPrice
            );
        };
    }

    public static Specification<Product> hasMaxPrice(BigDecimal maxPrice) {

        return (root, query, cb) -> {

            if (maxPrice == null) {
                return cb.conjunction();
            }

            return cb.lessThanOrEqualTo(
                    root.get("price"),
                    maxPrice
            );
        };
    }
}
