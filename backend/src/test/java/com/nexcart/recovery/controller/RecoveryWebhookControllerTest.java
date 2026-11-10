package com.nexcart.recovery.controller;

import com.nexcart.recovery.entity.ProcessedWebhookEvent;
import com.nexcart.recovery.repository.ProcessedWebhookEventRepository;
import com.nexcart.recovery.service.RecoveryService;
import com.nexcart.entity.Order;
import com.nexcart.entity.OrderStatus;
import com.nexcart.entity.Payment;
import com.nexcart.entity.PaymentStatus;
import com.nexcart.repository.OrderRepository;
import com.nexcart.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecoveryWebhookControllerTest {
    private static final String SECRET = "webhook-test-secret";
    private static final String BODY = "{\"event\":\"unhandled.event\",\"payload\":{}}";

    private final ProcessedWebhookEventRepository events = mock(ProcessedWebhookEventRepository.class);
    private final PaymentRepository payments = mock(PaymentRepository.class);
    private final OrderRepository orders = mock(OrderRepository.class);
    private final RecoveryService recovery = mock(RecoveryService.class);
    private final RecoveryWebhookController controller = new RecoveryWebhookController(
            payments, orders, recovery, events);

    RecoveryWebhookControllerTest() {
        ReflectionTestUtils.setField(controller, "webhookSecret", SECRET);
    }

    @Test
    void signedDuplicateEventIsAcknowledgedWithoutProcessingTwice() throws Exception {
        when(events.existsByEventId("evt_123")).thenReturn(false, true);
        String signature = signature(BODY);

        var first = controller.receive(BODY, signature, "evt_123");
        var duplicate = controller.receive(BODY, signature, "evt_123");

        assertEquals(HttpStatus.OK, first.getStatusCode());
        assertEquals("Processed", first.getBody().get("message"));
        assertEquals(HttpStatus.OK, duplicate.getStatusCode());
        assertEquals("Duplicate event ignored", duplicate.getBody().get("message"));
        verify(events).save(any(ProcessedWebhookEvent.class));
    }

    @Test
    void invalidSignatureIsRejectedWithoutRecordingEvent() throws Exception {
        var response = controller.receive(BODY, "invalid", "evt_invalid");

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(Map.of("message", "Invalid webhook signature"), response.getBody());
        verify(events, never()).save(any(ProcessedWebhookEvent.class));
    }

    @Test
    void signedCapturedPaymentConfirmsOrderAndMarksRecoveryOnce() throws Exception {
        String capturedBody = """
                {"event":"payment.captured","payload":{"payment":{"entity":{"id":"pay_123","order_id":"order_123","error_description":""}}}}
                """.trim();
        Order order = Order.builder().status(OrderStatus.PENDING).build();
        Payment payment = Payment.builder()
                .order(order)
                .amount(new BigDecimal("125.00"))
                .paymentStatus(PaymentStatus.PENDING)
                .razorpayOrderId("order_123")
                .build();
        when(events.existsByEventId("evt_captured")).thenReturn(false);
        when(payments.findByRazorpayOrderIdForUpdate("order_123")).thenReturn(Optional.of(payment));
        when(payments.save(payment)).thenReturn(payment);
        when(orders.save(order)).thenReturn(order);

        var response = controller.receive(capturedBody, signature(capturedBody), "evt_captured");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(PaymentStatus.SUCCESS, payment.getPaymentStatus());
        assertEquals("pay_123", payment.getRazorpayPaymentId());
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        assertEquals(true, payment.getPaidAt() != null && !payment.getPaidAt().isAfter(LocalDateTime.now()));
        verify(orders).save(order);
        verify(recovery).markRecovered(payment);
        verify(events).save(any(ProcessedWebhookEvent.class));
    }

    private static String signature(String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    }
}
