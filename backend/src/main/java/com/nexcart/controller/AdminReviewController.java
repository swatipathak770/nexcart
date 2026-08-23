package com.nexcart.controller;

import com.nexcart.dto.response.ReviewResponse;
import com.nexcart.service.AdminReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/reviews")
@RequiredArgsConstructor
@Tag(
        name = "Admin Review",
        description = "Admin Review Management APIs"
)
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReviewController {

    private final AdminReviewService adminReviewService;

    @GetMapping
    @Operation(summary = "Get All Reviews")
    public ResponseEntity<List<ReviewResponse>> getAllReviews() {

        return ResponseEntity.ok(
                adminReviewService.getAllReviews());
    }

    @GetMapping("/{reviewId}")
    @Operation(summary = "Get Review By ID")
    public ResponseEntity<ReviewResponse> getReviewById(
            @PathVariable Long reviewId) {

        return ResponseEntity.ok(
                adminReviewService.getReviewById(reviewId));
    }

    @DeleteMapping("/{reviewId}")
    @Operation(summary = "Delete Review")
    public ResponseEntity<String> deleteReview(
            @PathVariable Long reviewId) {

        adminReviewService.deleteReview(reviewId);

        return ResponseEntity.ok(
                "Review deleted successfully.");
    }
}
