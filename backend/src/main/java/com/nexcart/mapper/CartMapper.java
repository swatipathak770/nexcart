package com.nexcart.mapper;


import com.nexcart.dto.response.CartResponse;
import com.nexcart.entity.Cart;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class CartMapper {

    public CartResponse toResponse(Cart cart) {

        BigDecimal subtotal = cart.getProduct()
                .getPrice()
                .multiply(BigDecimal.valueOf(cart.getQuantity()));

        return CartResponse.builder()
                .cartId(cart.getId())
                .productId(cart.getProduct().getId())
                .productName(cart.getProduct().getName())
                .imageUrl(cart.getProduct().getImageUrl())
                .price(cart.getProduct().getPrice())
                .quantity(cart.getQuantity())
                .subtotal(subtotal)
                .build();
    }
}
