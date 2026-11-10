package com.nexcart.controller;

import com.nexcart.dto.request.ReviewRequest;
import com.nexcart.dto.response.ApiResponse;
import com.nexcart.dto.response.ReviewResponse;
import com.nexcart.dto.response.PagedResponse;
import com.nexcart.service.ReviewService;
import com.nexcart.util.PageableFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Set;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Tag(name = "Review", description = "Review and Rating Management APIs")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/products/{productId}")
    @Operation(summary = "Add Review", description = "Add a review for a product.")
    public ResponseEntity<ApiResponse<ReviewResponse>> addReview(
            @PathVariable Long productId,
            @Valid @RequestBody ReviewRequest request) {

        ReviewResponse response = reviewService.addReview(productId, request);

        ApiResponse<ReviewResponse> apiResponse = ApiResponse.<ReviewResponse>builder()
                .success(true)
                .message("Review added successfully.")
                .data(response)
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }

    @GetMapping("/products/{productId}")
    @Operation(summary = "Get Product Reviews", description = "Retrieve all reviews of a product.")
    public ResponseEntity<ApiResponse<PagedResponse<ReviewResponse>>> getReviewsByProduct(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Pageable pageable = PageableFactory.create(page, size, sortBy, direction,
                Set.of("id", "createdAt", "rating"));
        PagedResponse<ReviewResponse> response = PagedResponse.from(
                reviewService.getReviewsByProduct(productId, pageable));

        ApiResponse<PagedResponse<ReviewResponse>> apiResponse = ApiResponse.<PagedResponse<ReviewResponse>>builder()
                .success(true)
                .message("Reviews fetched successfully.")
                .data(response)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(apiResponse);
    }

    @PutMapping("/{reviewId}")
    @Operation(summary = "Update Review", description = "Update your review.")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewRequest request) {

        ReviewResponse response = reviewService.updateReview(reviewId, request);

        ApiResponse<ReviewResponse> apiResponse = ApiResponse.<ReviewResponse>builder()
                .success(true)
                .message("Review updated successfully.")
                .data(response)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(apiResponse);
    }

    @DeleteMapping("/{reviewId}")
    @Operation(summary = "Delete Review", description = "Delete your review.")
    public ResponseEntity<ApiResponse<Object>> deleteReview(
            @PathVariable Long reviewId) {

        reviewService.deleteReview(reviewId);

        ApiResponse<Object> apiResponse = ApiResponse.builder()
                .success(true)
                .message("Review deleted successfully.")
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(apiResponse);
    }
}
