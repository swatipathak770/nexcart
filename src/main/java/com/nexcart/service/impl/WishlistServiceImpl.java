package com.nexcart.service.impl;

import com.nexcart.dto.response.WishlistResponse;
import com.nexcart.entity.Product;
import com.nexcart.entity.User;
import com.nexcart.entity.Wishlist;
import com.nexcart.exception.ProductNotFoundException;
import com.nexcart.exception.WishlistAlreadyExistsException;
import com.nexcart.exception.WishlistNotFoundException;
import com.nexcart.mapper.WishlistMapper;
import com.nexcart.repository.ProductRepository;
import com.nexcart.repository.UserRepository;
import com.nexcart.repository.WishlistRepository;
import com.nexcart.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final WishlistMapper wishlistMapper;

    @Override
    public WishlistResponse addToWishlist(Long productId) {

        User user = getCurrentUser();

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new ProductNotFoundException("Product not found."));

        if (wishlistRepository.existsByUserAndProduct(user, product)) {
            throw new WishlistAlreadyExistsException("Product already exists in wishlist.");
        }

        Wishlist wishlist = Wishlist.builder()
                .user(user)
                .product(product)
                .build();

        Wishlist savedWishlist = wishlistRepository.save(wishlist);

        return wishlistMapper.toResponse(savedWishlist);
    }

    @Override
    public List<WishlistResponse> getMyWishlist() {

        User user = getCurrentUser();

        return wishlistRepository.findByUser(user)
                .stream()
                .map(wishlistMapper::toResponse)
                .toList();
    }

    @Override
    public void removeFromWishlist(Long wishlistId) {

        User user = getCurrentUser();

        Wishlist wishlist = wishlistRepository.findById(wishlistId)
                .orElseThrow(() ->
                        new WishlistNotFoundException("Wishlist item not found."));

        if (!wishlist.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You are not authorized to remove this wishlist item.");
        }

        wishlistRepository.delete(wishlist);
    }

    private User getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found."));
    }
}
