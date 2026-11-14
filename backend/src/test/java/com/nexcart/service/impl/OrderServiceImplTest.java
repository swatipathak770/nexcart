package com.nexcart.service.impl;

import com.nexcart.entity.*;
import com.nexcart.exception.ResourceNotFoundException;
import com.nexcart.mapper.OrderMapper;
import com.nexcart.recovery.service.RecoveryService;
import com.nexcart.repository.*;
import com.nexcart.service.CouponService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderServiceImplTest {
    private static final String OWNER_EMAIL = "owner@example.test";
    private final OrderRepository orders = mock(OrderRepository.class);
    private final CartRepository carts = mock(CartRepository.class);
    private final ProductRepository products = mock(ProductRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final OrderMapper mapper = mock(OrderMapper.class);
    private final CouponService coupons = mock(CouponService.class);
    private final RecoveryService recovery = mock(RecoveryService.class);
    private final PaymentRepository payments = mock(PaymentRepository.class);
    private final PaymentAttemptRepository attempts = mock(PaymentAttemptRepository.class);
    private final CheckoutIdempotencyKeyRepository idempotencyKeys = mock(CheckoutIdempotencyKeyRepository.class);
    private final AddressRepository addresses = mock(AddressRepository.class);
    private final OrderServiceImpl service = new OrderServiceImpl(
            orders, carts, products, users, mapper, coupons, recovery, payments, attempts, idempotencyKeys, addresses);

    private User owner;
    private Order order;
    private Product product;
    private Payment payment;
    private PaymentAttempt attempt;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(12L).fullName("Order Owner").email(OWNER_EMAIL).password("unused")
                .phoneNumber("1234567890").role(Role.CUSTOMER).build();
        product = Product.builder().id(5L).name("Test product").sku("TEST-5")
                .price(new BigDecimal("10.00")).stock(3).build();
        order = Order.builder().user(owner).status(OrderStatus.PENDING).totalAmount(new BigDecimal("10.00"))
                .discountAmount(BigDecimal.ZERO).finalAmount(new BigDecimal("10.00")).build();
        order.setId(32L);
        order.setOrderItems(new java.util.ArrayList<>(List.of(OrderItem.builder()
                .order(order).product(product).quantity(1).price(new BigDecimal("10.00")).build())));
        payment = Payment.builder().id(9L).order(order).amount(new BigDecimal("10.00"))
                .paymentStatus(PaymentStatus.PENDING).razorpayOrderId("order_test").build();
        attempt = PaymentAttempt.builder().id(18L).payment(payment).attemptNumber(1)
                .amount(payment.getAmount()).status(PaymentStatus.PENDING).razorpayOrderId("order_test").build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(OWNER_EMAIL, null));
        when(users.findByEmail(OWNER_EMAIL)).thenReturn(Optional.of(owner));
        when(orders.findByIdForUpdate(32L)).thenReturn(Optional.of(order));
        when(payments.findByOrderIdForUpdate(32L)).thenReturn(Optional.of(payment));
        when(products.findByIdForUpdate(5L)).thenReturn(Optional.of(product));
        when(products.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orders.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(payments.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(attempts.findByPaymentIdAndStatusForUpdate(9L, PaymentStatus.PENDING)).thenReturn(List.of(attempt));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void pendingOrderCancellationRestoresStockAndClosesPaymentAttempt() {
        service.cancelOrder(32L);

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(4, product.getStock());
        assertEquals(PaymentStatus.CANCELLED, payment.getPaymentStatus());
        assertEquals(PaymentStatus.CANCELLED, attempt.getStatus());
        verify(orders).save(order);
        verify(payments).save(payment);
        verify(attempts).save(attempt);
        verify(recovery).cancelForOrder(order, "Customer cancelled this order.");
    }

    @Test
    void alreadyCancelledOrderReturnsConflictWithoutRestockingAgain() {
        order.setStatus(OrderStatus.CANCELLED);
        assertThrows(IllegalStateException.class, () -> service.cancelOrder(32L));
        assertEquals(3, product.getStock());
        verifyNoInteractions(products, attempts, recovery);
    }

    @Test
    void confirmedOrderCannotBeCancelled() {
        order.setStatus(OrderStatus.CONFIRMED);
        assertThrows(IllegalStateException.class, () -> service.cancelOrder(32L));
        verifyNoInteractions(products, attempts, recovery);
    }

    @Test
    void anotherCustomersOrderIsForbidden() {
        User other = User.builder().id(13L).fullName("Other").email("other@example.test")
                .password("unused").phoneNumber("1234567890").role(Role.CUSTOMER).build();
        when(users.findByEmail(OWNER_EMAIL)).thenReturn(Optional.of(other));
        assertThrows(AccessDeniedException.class, () -> service.cancelOrder(32L));
        verifyNoInteractions(products, payments, attempts, recovery);
    }

    @Test
    void missingOrderReturnsNotFound() {
        when(orders.findByIdForUpdate(999L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.cancelOrder(999L));
        verifyNoInteractions(products, payments, attempts, recovery);
    }
}
