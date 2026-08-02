package com.nexcart.controller;

import com.nexcart.dto.response.WishlistResponse;
import com.nexcart.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
@Tag(name = "Wishlist Management", description = "APIs for managing user wishlist")
@SecurityRequirement(name = "bearerAuth")
public class WishlistController {

    private final WishlistService wishlistService;

    @Operation(summary = "Add a product to wishlist")
    @PostMapping("/{productId}")
    public ResponseEntity<WishlistResponse> addToWishlist(
            @PathVariable Long productId) {

        WishlistResponse response = wishlistService.addToWishlist(productId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get logged-in user's wishlist")
    @GetMapping
    public ResponseEntity<List<WishlistResponse>> getMyWishlist() {

        return ResponseEntity.ok(wishlistService.getMyWishlist());
    }

    @Operation(summary = "Remove product from wishlist")
    @DeleteMapping("/{wishlistId}")
    public ResponseEntity<String> removeFromWishlist(
            @PathVariable Long wishlistId) {

        wishlistService.removeFromWishlist(wishlistId);

        return ResponseEntity.ok("Product removed from wishlist successfully.");
    }
}
