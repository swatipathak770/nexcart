package com.nexcart.controller;

import com.nexcart.dto.request.CartRequest;
import com.nexcart.dto.response.CartResponse;
import com.nexcart.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping
    public ResponseEntity<CartResponse> addToCart(
            @Valid @RequestBody CartRequest request) {

        CartResponse response = cartService.addToCart(request);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    @GetMapping
    public ResponseEntity<List<CartResponse>> getMyCart() {

        return ResponseEntity.ok(cartService.getMyCart());
    }
    @PutMapping("/{cartId}")
    public ResponseEntity<CartResponse> updateQuantity(
            @PathVariable Long cartId,
            @RequestParam Integer quantity) {

        return ResponseEntity.ok(
                cartService.updateQuantity(cartId, quantity)
        );
    }

    @DeleteMapping
    public ResponseEntity<String> clearCart() {

        cartService.clearCart();

        return ResponseEntity.ok("Cart cleared successfully.");
    }
    @DeleteMapping("/{cartId}")
    public ResponseEntity<String> removeCartItem(
            @PathVariable Long cartId) {

        cartService.removeCartItem(cartId);

        return ResponseEntity.ok("Item removed from cart successfully.");
    }
}
