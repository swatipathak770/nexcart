package com.nexcart.service.impl;

import com.nexcart.dto.request.CartRequest;
import com.nexcart.entity.Cart;
import com.nexcart.entity.Product;
import com.nexcart.entity.User;
import com.nexcart.mapper.CartMapper;
import com.nexcart.repository.CartRepository;
import com.nexcart.repository.ProductRepository;
import com.nexcart.repository.UserRepository;
import com.nexcart.repository.WishlistRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CartServiceImplTest {
    private final CartRepository carts = mock(CartRepository.class);
    private final ProductRepository products = mock(ProductRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final WishlistRepository wishlists = mock(WishlistRepository.class);
    private final CartServiceImpl service = new CartServiceImpl(carts, products, users, new CartMapper(), wishlists);
    private final User user = User.builder().id(10L).email("customer@example.test").build();
    private final Product product = Product.builder().id(20L).name("Keyboard")
            .price(new BigDecimal("25.00")).stock(5).sku("KB-1").build();

    @BeforeEach
    void authenticateCustomer() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getEmail(), null));
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void addToCartRejectsQuantityAboveAvailableStock() {
        when(products.findById(product.getId())).thenReturn(Optional.of(product));
        Cart existing = Cart.builder().user(user).product(product).quantity(3).build();
        when(carts.findByUserAndProduct(user, product)).thenReturn(Optional.of(existing));

        assertThrows(IllegalArgumentException.class,
                () -> service.addToCart(new CartRequest(product.getId(), 3)));

        verify(carts, never()).save(any());
        verifyNoInteractions(wishlists);
    }

    @Test
    void addToCartAccumulatesValidQuantityAndRemovesWishlistEntry() {
        when(products.findById(product.getId())).thenReturn(Optional.of(product));
        when(carts.findByUserAndProduct(user, product)).thenReturn(Optional.empty());
        when(carts.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.addToCart(new CartRequest(product.getId(), 2));

        assertEquals(2, response.getQuantity());
        assertEquals(new BigDecimal("50.00"), response.getSubtotal());
        verify(wishlists).deleteByUserAndProduct(user, product);
    }

    @Test
    void updateQuantityRequiresPositiveValueAndCannotExceedStock() {
        assertThrows(IllegalArgumentException.class, () -> service.updateQuantity(1L, 0));
        assertThrows(IllegalArgumentException.class, () -> service.updateQuantity(1L, -1));
        verifyNoInteractions(carts);

        Cart cart = Cart.builder().user(user).product(product).quantity(1).build();
        when(carts.findById(1L)).thenReturn(Optional.of(cart));
        assertThrows(IllegalArgumentException.class, () -> service.updateQuantity(1L, 6));
        verify(carts, never()).save(any());
    }

    @Test
    void customerCannotChangeAnotherUsersCart() {
        User other = User.builder().id(11L).email("other@example.test").build();
        Cart cart = Cart.builder().user(other).product(product).quantity(1).build();
        when(carts.findById(1L)).thenReturn(Optional.of(cart));

        assertThrows(RuntimeException.class, () -> service.updateQuantity(1L, 2));
        verify(carts, never()).save(any());
    }
}
