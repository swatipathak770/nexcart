package com.nexcart.service.impl;

import com.nexcart.entity.Cart;
import com.nexcart.entity.Product;
import com.nexcart.entity.Role;
import com.nexcart.entity.User;
import com.nexcart.mapper.OrderMapper;
import com.nexcart.recovery.service.RecoveryService;
import com.nexcart.repository.CartRepository;
import com.nexcart.repository.OrderRepository;
import com.nexcart.repository.ProductRepository;
import com.nexcart.repository.UserRepository;
import com.nexcart.service.CouponService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceInventoryLockTest {
    private final OrderRepository orders = mock(OrderRepository.class);
    private final CartRepository carts = mock(CartRepository.class);
    private final ProductRepository products = mock(ProductRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final OrderServiceImpl service = new OrderServiceImpl(
            orders, carts, products, users, new OrderMapper(), mock(CouponService.class), mock(RecoveryService.class));

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void checkoutLocksProductBeforeCheckingAndDecrementingStock() {
        String email = "checkout@example.test";
        User user = User.builder().id(7L).fullName("Test User").email(email).password("unused")
                .phoneNumber("1234567890").role(Role.CUSTOMER).build();
        Product product = Product.builder().id(11L).name("Test item").sku("TEST-11")
                .price(new BigDecimal("12.50")).stock(3).imageUrl("image").build();
        Cart cart = Cart.builder().user(user).product(product).quantity(2).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null, List.of()));
        when(users.findByEmail(email)).thenReturn(Optional.of(user));
        when(carts.findByUserForCheckout(user)).thenReturn(List.of(cart));
        when(products.findByIdForUpdate(11L)).thenReturn(Optional.of(product));
        when(orders.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.placeOrder(null);

        verify(products).findByIdForUpdate(11L);
        assertEquals(1, product.getStock());
    }
}
