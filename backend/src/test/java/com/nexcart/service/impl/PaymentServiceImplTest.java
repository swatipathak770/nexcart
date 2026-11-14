package com.nexcart.service.impl;

import com.nexcart.dto.request.PaymentFailureRequest;
import com.nexcart.dto.request.PaymentVerificationRequest;
import com.nexcart.entity.Order;
import com.nexcart.entity.OrderStatus;
import com.nexcart.entity.Payment;
import com.nexcart.entity.PaymentStatus;
import com.nexcart.entity.Role;
import com.nexcart.entity.User;
import com.nexcart.recovery.service.RecoveryService;
import com.nexcart.repository.OrderRepository;
import com.nexcart.repository.PaymentRepository;
import com.nexcart.repository.PaymentAttemptRepository;
import com.nexcart.entity.PaymentAttempt;
import com.razorpay.RazorpayClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PaymentServiceImplTest {
    private static final String EMAIL = "payment@example.test";
    private static final String KEY_SECRET = "unit-test-secret";

    private final PaymentRepository payments = mock(PaymentRepository.class);
    private final PaymentAttemptRepository attempts = mock(PaymentAttemptRepository.class);
    private final OrderRepository orders = mock(OrderRepository.class);
    private final RazorpayClient razorpay = mock(RazorpayClient.class);
    private final RecoveryService recovery = mock(RecoveryService.class);
    private final PaymentServiceImpl service = new PaymentServiceImpl(payments, attempts, orders, razorpay, recovery);

    private Order order;
    private Payment payment;
    private PaymentAttempt attempt;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "razorpayKeySecret", KEY_SECRET);
        User user = User.builder().id(12L).fullName("Payment User").email(EMAIL)
                .password("unused").phoneNumber("1234567890").role(Role.CUSTOMER).build();
        order = Order.builder().user(user).status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("25.00")).discountAmount(BigDecimal.ZERO)
                .finalAmount(new BigDecimal("25.00")).build();
        order.setId(31L);
        payment = Payment.builder().id(41L).order(order).amount(new BigDecimal("25.00"))
                .paymentStatus(PaymentStatus.PENDING).razorpayOrderId("order_test_31").build();
        attempt = PaymentAttempt.builder().id(51L).payment(payment).attemptNumber(1).amount(payment.getAmount())
                .status(PaymentStatus.PENDING).razorpayOrderId("order_test_31").build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(EMAIL, null));
        when(orders.findById(31L)).thenReturn(Optional.of(order));
        when(orders.findByIdForUpdate(31L)).thenReturn(Optional.of(order));
        when(payments.findByOrderId(31L)).thenReturn(Optional.of(payment));
        when(payments.findByOrderIdForUpdate(31L)).thenReturn(Optional.of(payment));
        when(attempts.findByRazorpayOrderIdForUpdate("order_test_31")).thenReturn(Optional.of(attempt));
        when(attempts.findByRazorpayPaymentId(any())).thenReturn(Optional.empty());
        when(attempts.findByRazorpayOrderId("order_test_31")).thenReturn(Optional.of(attempt));
        when(payments.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orders.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validSignatureConfirmsOrderAndMarksPaymentSuccessful() throws Exception {
        String paymentId = "pay_test_31";
        PaymentVerificationRequest request = verificationRequest(paymentId, signature("order_test_31", paymentId));
        when(payments.findByRazorpayPaymentId(paymentId)).thenReturn(Optional.empty());

        service.verifyPayment(request);

        assertEquals(PaymentStatus.SUCCESS, payment.getPaymentStatus());
        assertEquals(PaymentStatus.SUCCESS, attempt.getStatus());
        assertEquals(paymentId, payment.getRazorpayPaymentId());
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        verify(orders).save(order);
        verify(recovery).markRecovered(payment);
    }

    @Test
    void invalidSignaturePersistsFailureAndStartsRecovery() {
        PaymentVerificationRequest request = verificationRequest("pay_test_bad", "invalid-signature");
        when(payments.findByRazorpayPaymentId("pay_test_bad")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.verifyPayment(request));

        assertEquals(PaymentStatus.FAILED, payment.getPaymentStatus());
        assertEquals(PaymentStatus.FAILED, attempt.getStatus());
        assertEquals("Razorpay signature verification failed.", payment.getFailureReason());
        verify(payments).save(payment);
        verify(recovery).detectFailedPayment(payment);
        verify(orders, never()).save(order);
    }

    @Test
    void duplicateRazorpayPaymentIdIsRejectedBeforeAnyPaymentUpdate() {
        PaymentVerificationRequest request = verificationRequest("pay_reused", "signature");
        when(payments.findByRazorpayPaymentId("pay_reused")).thenReturn(Optional.of(payment));

        assertThrows(IllegalArgumentException.class, () -> service.verifyPayment(request));

        assertEquals(PaymentStatus.PENDING, payment.getPaymentStatus());
        verify(payments, never()).save(any(Payment.class));
    }

    @Test
    void retryCheckoutIsRestrictedToTheOrderOwner() {
        order.getUser().setEmail("different-owner@example.test");

        assertThrows(AccessDeniedException.class, () -> service.getRetryCheckout(31L));
        verifyNoInteractions(recovery);
    }

    @Test
    void customerCancellationUpdatesOnlyPendingPayment() {
        PaymentFailureRequest request = new PaymentFailureRequest();
        request.setOrderId(31L);

        service.cancelPayment(request);

        assertEquals(PaymentStatus.CANCELLED, payment.getPaymentStatus());
        assertEquals(PaymentStatus.CANCELLED, attempt.getStatus());
        assertEquals("Payment cancelled by customer.", payment.getFailureReason());
        verify(recovery, never()).detectFailedPayment(any(Payment.class));
    }

    @Test
    void gatewayFailurePreservesFailedAttemptAndLeavesOrderPending() {
        PaymentFailureRequest request = new PaymentFailureRequest();
        request.setOrderId(31L);
        request.setReason("Test gateway decline");

        service.markPaymentFailed(request);

        assertEquals(PaymentStatus.FAILED, payment.getPaymentStatus());
        assertEquals(PaymentStatus.FAILED, attempt.getStatus());
        assertEquals(OrderStatus.PENDING, order.getStatus());
        verify(recovery).detectFailedPayment(payment);
        verify(orders, never()).save(order);
    }

    private PaymentVerificationRequest verificationRequest(String paymentId, String signature) {
        PaymentVerificationRequest request = new PaymentVerificationRequest();
        request.setOrderId(31L);
        request.setRazorpayPaymentId(paymentId);
        request.setRazorpaySignature(signature);
        return request;
    }

    private String signature(String razorpayOrderId, String paymentId) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(KEY_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal((razorpayOrderId + "|" + paymentId).getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }
}
