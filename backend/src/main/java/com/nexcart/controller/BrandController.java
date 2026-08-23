package com.nexcart.controller;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.nexcart.dto.request.BrandRequest;
import com.nexcart.dto.response.ApiResponse;
import com.nexcart.dto.response.BrandResponse;
import com.nexcart.service.BrandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
@Tag(
        name = "Brand Management",
        description = "APIs for managing product brands"
)
@RestController
@RequestMapping("/api/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;
    @Operation(summary = "Create a new brand")
    @PostMapping
    public ResponseEntity<ApiResponse<BrandResponse>> createBrand(
            @Valid @RequestBody BrandRequest request) {

        BrandResponse response = brandService.createBrand(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<BrandResponse>builder()
                        .success(true)
                        .message("Brand created successfully")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build());
    }
    @Operation(summary = "Get all brands")
    @GetMapping
    public ResponseEntity<ApiResponse<List<BrandResponse>>> getAllBrands() {

        List<BrandResponse> response = brandService.getAllBrands();

        return ResponseEntity.ok(
                ApiResponse.<List<BrandResponse>>builder()
                        .success(true)
                        .message("Brands fetched successfully")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build());
    }
    @Operation(summary = "Get brand by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BrandResponse>> getBrandById(
            @PathVariable Long id) {

        BrandResponse response = brandService.getBrandById(id);

        return ResponseEntity.ok(
                ApiResponse.<BrandResponse>builder()
                        .success(true)
                        .message("Brand fetched successfully")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build());
    }
    @Operation(summary = "Update an existing brand")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BrandResponse>> updateBrand(
            @PathVariable Long id,
            @Valid @RequestBody BrandRequest request) {

        BrandResponse response = brandService.updateBrand(id, request);

        return ResponseEntity.ok(
                ApiResponse.<BrandResponse>builder()
                        .success(true)
                        .message("Brand updated successfully")
                        .data(response)
                        .timestamp(LocalDateTime.now())
                        .build());
    }
    @Operation(summary = "Delete a brand")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteBrand(
            @PathVariable Long id) {

        brandService.deleteBrand(id);

        return ResponseEntity.ok(
                ApiResponse.builder()
                        .success(true)
                        .message("Brand deleted successfully")
                        .timestamp(LocalDateTime.now())
                        .build());
    }
}
