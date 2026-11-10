package com.nexcart.service;

import com.nexcart.dto.request.ProductRequest;
import com.nexcart.dto.response.ProductResponse;
import com.nexcart.dto.response.PagedResponse;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface ProductService {

    ProductResponse createProduct(ProductRequest request);

    PagedResponse<ProductResponse> getAllProducts(Pageable pageable);

    ProductResponse getProductById(Long id);

    ProductResponse updateProduct(Long id, ProductRequest request);

    void deleteProduct(Long id);

    PagedResponse<ProductResponse> searchProducts(String keyword, Pageable pageable);
    PagedResponse<ProductResponse> filterProducts(
            String keyword,
            String category,
            String brand,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable
    );
}
