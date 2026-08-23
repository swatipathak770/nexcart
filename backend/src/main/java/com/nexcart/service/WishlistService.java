package com.nexcart.service;

import com.nexcart.dto.response.WishlistResponse;
import com.nexcart.entity.Wishlist;
import com.nexcart.exception.WishlistAlreadyExistsException;
import com.nexcart.exception.WishlistNotFoundException;

import java.util.List;

public interface WishlistService {

    WishlistResponse addToWishlist(Long productId);

    List<WishlistResponse> getMyWishlist();

    void removeFromWishlist(Long wishlistId);


}
