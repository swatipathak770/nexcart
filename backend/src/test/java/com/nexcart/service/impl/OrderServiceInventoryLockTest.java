package com.nexcart.service.impl;

import com.nexcart.entity.Cart;
import com.nexcart.entity.Address;
import com.nexcart.entity.Product;
import com.nexcart.entity.Role;
import com.nexcart.entity.User;
import com.nexcart.mapper.OrderMapper;
import com.nexcart.recovery.service.RecoveryService;
import com.nexcart.repository.CartRepository;
import com.nexcart.repository.OrderRepository;
import com.nexcart.repository.ProductRepository;
import com.nexcart.repository.UserRepository;
import com.nexcart.repository.PaymentRepository;
import com.nexcart.repository.PaymentAttemptRepository;
import com.nexcart.repository.CheckoutIdempotencyKeyRepository;
import com.nexcart.repository.AddressRepository;
import com.nexcart.entity.CheckoutIdempotencyKey;
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
    private final CheckoutIdempotencyKeyRepository idempotencyKeys = mock(CheckoutIdempotencyKeyRepository.class);
    private final AddressRepository addresses = mock(AddressRepository.class);
    private final OrderServiceImpl service = new OrderServiceImpl(
            orders, carts, products, users, new OrderMapper(), mock(CouponService.class), mock(RecoveryService.class),
            mock(PaymentRepository.class), mock(PaymentAttemptRepository.class), idempotencyKeys, addresses);

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
        when(users.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
        when(carts.findByUserForCheckout(user)).thenReturn(List.of(cart));
        when(idempotencyKeys.findByUserAndKeyForUpdate(user.getId(), "checkout-key-1234567890")).thenReturn(Optional.empty());
        when(idempotencyKeys.saveAndFlush(any(CheckoutIdempotencyKey.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(addresses.findByIdAndUser(23L, user)).thenReturn(Optional.of(Address.builder()
                .fullName("Test User").phoneNumber("1234567890").addressLine1("1 Test Street")
                .city("Test City").state("Test State").country("India").postalCode("123456").build()));
        when(products.findByIdForUpdate(11L)).thenReturn(Optional.of(product));
        when(orders.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.placeOrder(null, "checkout-key-1234567890", 23L);

        verify(products).findByIdForUpdate(11L);
        assertEquals(1, product.getStock());
        assertEquals("1 Test Street", ordersArgument().getDeliveryAddress().getAddressLine1());
    }

    private com.nexcart.entity.Order ordersArgument() {
        var captor = org.mockito.ArgumentCaptor.forClass(com.nexcart.entity.Order.class);
        verify(orders).save(captor.capture());
        return captor.getValue();
    }

}
