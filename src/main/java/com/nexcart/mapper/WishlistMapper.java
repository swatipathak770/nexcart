package com.nexcart.mapper;

import com.nexcart.dto.response.WishlistResponse;
import com.nexcart.entity.Wishlist;
import org.springframework.stereotype.Component;

@Component
public class WishlistMapper {

    public WishlistResponse toResponse(Wishlist wishlist) {

        return WishlistResponse.builder()
                .wishlistId(wishlist.getId())
                .productId(wishlist.getProduct().getId())
                .productName(wishlist.getProduct().getName())
                .imageUrl(wishlist.getProduct().getImageUrl())
                .price(wishlist.getProduct().getPrice())
                .categoryName(wishlist.getProduct().getCategory().getName())
                .brandName(wishlist.getProduct().getBrand().getName())
                .build();
    }
}
