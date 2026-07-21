package com.nexcart.service;

import com.nexcart.dto.request.CartRequest;
import com.nexcart.dto.response.CartResponse;

import java.util.List;

public interface CartService {

    CartResponse addToCart(CartRequest request);

    List<CartResponse> getMyCart();

    CartResponse updateQuantity(Long cartId, Integer quantity);

    void removeFromCart(Long cartId);

    void clearCart();
}