package com.nexcart.service.impl;

import com.nexcart.dto.request.CartRequest;
import com.nexcart.dto.response.CartResponse;
import com.nexcart.entity.Cart;
import com.nexcart.entity.Product;
import com.nexcart.entity.User;
import com.nexcart.exception.ResourceNotFoundException;
import com.nexcart.mapper.CartMapper;
import com.nexcart.repository.CartRepository;
import com.nexcart.repository.ProductRepository;
import com.nexcart.repository.UserRepository;
import com.nexcart.repository.WishlistRepository;
import com.nexcart.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartMapper cartMapper;
    private final WishlistRepository wishlistRepository;


    private User getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));
    }


    @Override
    @Transactional
    public CartResponse addToCart(CartRequest request) {

        User user = getCurrentUser();

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product not found"));

        Cart cart = cartRepository
                .findByUserAndProduct(user, product)
                .orElse(null);

        if (cart != null) {

            // Product already exists in cart, update quantity
            cart.setQuantity(cart.getQuantity() + request.getQuantity());

        } else {

            // Product not in cart, create new cart item
            cart = Cart.builder()
                    .user(user)
                    .product(product)
                    .quantity(request.getQuantity())
                    .build();
        }

        cart = cartRepository.save(cart);
        wishlistRepository.deleteByUserAndProduct(user, product);

        return cartMapper.toResponse(cart);
    }


    @Override
    public List<CartResponse> getMyCart() {

        User user = getCurrentUser();

        return cartRepository.findByUser(user)
                .stream()
                .map(cartMapper::toResponse)
                .toList();
    }


    @Transactional
    @Override
    public CartResponse updateQuantity(Long cartId, Integer quantity) {

        User user = getCurrentUser();

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Cart item not found"));

        if (!cart.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You are not authorized.");
        }

        cart.setQuantity(quantity);

        cart = cartRepository.save(cart);

        return cartMapper.toResponse(cart);
    }

    @Override
    @Transactional
    public void removeCartItem(Long cartId) {

        User user = getCurrentUser();

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Cart item not found"));

        if (!cart.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You are not authorized to delete this cart item.");
        }

        cartRepository.delete(cart);
    }

    @Override
    @Transactional
    public void clearCart() {

        User user = getCurrentUser();

        cartRepository.deleteByUser(user);
    }


}
