package com.nexcart.controller;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.nexcart.dto.request.ProductRequest;
import com.nexcart.dto.response.ApiResponse;
import com.nexcart.dto.response.ProductResponse;
import com.nexcart.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
@Tag(
        name = "Product Management",
        description = "APIs for managing products"
)
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    @Operation(summary = "Create a new product")
    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductRequest request) {

        ProductResponse response = productService.createProduct(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<ProductResponse>builder()
                        .success(true)
                        .message("Product created successfully")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build());
    }
    @Operation(summary = "Get all products with pagination and sorting")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getAllProducts(

            @RequestParam(defaultValue = "0") int page,

            @RequestParam(defaultValue = "10") int size,

            @RequestParam(defaultValue = "id") String sortBy,

            @RequestParam(defaultValue = "asc") String direction) {

        Page<ProductResponse> response =
                productService.getAllProducts(
                        page,
                        size,
                        sortBy,
                        direction
                );

        return ResponseEntity.ok(
                ApiResponse.<Page<ProductResponse>>builder()
                        .success(true)
                        .message("Products fetched successfully")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }
    @Operation(summary = "Get product by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(
            @PathVariable Long id) {

        ProductResponse response = productService.getProductById(id);

        return ResponseEntity.ok(
                ApiResponse.<ProductResponse>builder()
                        .success(true)
                        .message("Product fetched successfully")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build());
    }
    @Operation(summary = "Update an existing product")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {

        ProductResponse response =
                productService.updateProduct(id, request);

        return ResponseEntity.ok(
                ApiResponse.<ProductResponse>builder()
                        .success(true)
                        .message("Product updated successfully")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build());
    }
    @Operation(summary = "Delete a product")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteProduct(
            @PathVariable Long id) {

        productService.deleteProduct(id);

        return ResponseEntity.ok(
                ApiResponse.builder()
                        .success(true)
                        .message("Product deleted successfully")
                        .timestamp(LocalDateTime.now())
                        .build());
    }
    @Operation(summary = "Search products by name")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> searchProducts(
            @RequestParam String keyword) {

        List<ProductResponse> response =
                productService.searchProducts(keyword);

        return ResponseEntity.ok(
                ApiResponse.<List<ProductResponse>>builder()
                        .success(true)
                        .message("Products fetched successfully")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }
    @Operation(summary = "Filter products")
    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> filterProducts(

            @RequestParam(required = false) String keyword,

            @RequestParam(required = false) String category,

            @RequestParam(required = false) String brand,

            @RequestParam(required = false) BigDecimal minPrice,

            @RequestParam(required = false) BigDecimal maxPrice,

            @RequestParam(defaultValue = "0") int page,

            @RequestParam(defaultValue = "10") int size,

            @RequestParam(defaultValue = "id") String sortBy,

            @RequestParam(defaultValue = "asc") String direction) {

        Page<ProductResponse> response =
                productService.filterProducts(
                        keyword,
                        category,
                        brand,
                        minPrice,
                        maxPrice,
                        page,
                        size,
                        sortBy,
                        direction
                );

        return ResponseEntity.ok(
                ApiResponse.<Page<ProductResponse>>builder()
                        .success(true)
                        .message("Products fetched successfully")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }
}
