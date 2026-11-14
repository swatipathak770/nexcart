package com.nexcart.recovery.controller;

import com.nexcart.recovery.entity.ProcessedWebhookEvent;
import com.nexcart.recovery.repository.ProcessedWebhookEventRepository;
import com.nexcart.recovery.service.RecoveryService;
import com.nexcart.entity.Order;
import com.nexcart.entity.OrderStatus;
import com.nexcart.entity.Payment;
import com.nexcart.entity.PaymentStatus;
import com.nexcart.entity.PaymentAttempt;
import com.nexcart.repository.OrderRepository;
import com.nexcart.repository.PaymentRepository;
import com.nexcart.repository.PaymentAttemptRepository;
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
    private final PaymentAttemptRepository paymentAttempts = mock(PaymentAttemptRepository.class);
    private final OrderRepository orders = mock(OrderRepository.class);
    private final RecoveryService recovery = mock(RecoveryService.class);
    private final RecoveryWebhookController controller = new RecoveryWebhookController(
            payments, paymentAttempts, orders, recovery, events);

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
        order.setId(123L);
        Payment payment = Payment.builder()
                .order(order)
                .amount(new BigDecimal("125.00"))
                .paymentStatus(PaymentStatus.PENDING)
                .razorpayOrderId("order_123")
                .build();
        payment.setId(456L);
        PaymentAttempt attempt = PaymentAttempt.builder().id(789L).payment(payment).attemptNumber(1)
                .amount(payment.getAmount()).status(PaymentStatus.PENDING).razorpayOrderId("order_123").build();
        when(events.existsByEventId("evt_captured")).thenReturn(false);
        when(paymentAttempts.findByRazorpayOrderId("order_123")).thenReturn(Optional.of(attempt));
        when(paymentAttempts.findByRazorpayOrderIdForUpdate("order_123")).thenReturn(Optional.of(attempt));
        when(orders.findByIdForUpdate(123L)).thenReturn(Optional.of(order));
        when(payments.findByOrderIdForUpdate(123L)).thenReturn(Optional.of(payment));
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

    @Test
    void duplicateCapturedWebhookDoesNotApplyPaymentTwice() throws Exception {
        Fixture fixture = fixture(OrderStatus.PENDING, PaymentStatus.PENDING, PaymentStatus.PENDING,
                "order_123", "pay_123");
        String body = captured("pay_123", "order_123");
        when(events.existsByEventId("evt_capture")).thenReturn(false, true);
        stubFixture(fixture, "order_123");

        var first = controller.receive(body, signature(body), "evt_capture");
        var duplicate = controller.receive(body, signature(body), "evt_capture");

        assertEquals(HttpStatus.OK, first.getStatusCode());
        assertEquals("Duplicate event ignored", duplicate.getBody().get("message"));
        assertEquals(OrderStatus.CONFIRMED, fixture.order().getStatus());
        assertEquals(PaymentStatus.SUCCESS, fixture.attempt().getStatus());
        verify(recovery).markRecovered(fixture.payment());
    }

    @Test
    void capturedPaymentAfterCancellationIsRecordedAndFlaggedWithoutReconfirmingOrder() throws Exception {
        Fixture fixture = fixture(OrderStatus.CANCELLED, PaymentStatus.CANCELLED, PaymentStatus.CANCELLED,
                "order_123", "pay_late");
        String body = captured("pay_late", "order_123");
        when(events.existsByEventId("evt_late")).thenReturn(false);
        stubFixture(fixture, "order_123");

        var response = controller.receive(body, signature(body), "evt_late");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(OrderStatus.CANCELLED, fixture.order().getStatus());
        assertEquals(PaymentStatus.SUCCESS, fixture.payment().getPaymentStatus());
        assertEquals(PaymentStatus.SUCCESS, fixture.attempt().getStatus());
        org.junit.jupiter.api.Assertions.assertTrue(fixture.payment().getFailureReason().contains("no refund has been recorded"));
        verify(recovery).recordCaptureReconciliation(fixture.order(), fixture.attempt(), fixture.payment().getFailureReason());
        verify(recovery, never()).markRecovered(fixture.payment());
        verify(orders, never()).save(fixture.order());
    }

    @Test
    void separateCaptureForAnotherAttemptIsFlaggedAndDoesNotOverwriteFirstCapture() throws Exception {
        Fixture fixture = fixture(OrderStatus.CONFIRMED, PaymentStatus.SUCCESS, PaymentStatus.PENDING,
                "order_retry", "pay_second");
        fixture.payment().setRazorpayOrderId("order_first");
        fixture.payment().setRazorpayPaymentId("pay_first");
        fixture.payment().setPaidAt(LocalDateTime.now());
        String body = captured("pay_second", "order_retry");
        when(events.existsByEventId("evt_second")).thenReturn(false);
        stubFixture(fixture, "order_retry");

        controller.receive(body, signature(body), "evt_second");

        assertEquals(OrderStatus.CONFIRMED, fixture.order().getStatus());
        assertEquals("pay_first", fixture.payment().getRazorpayPaymentId());
        assertEquals(PaymentStatus.SUCCESS, fixture.attempt().getStatus());
        org.junit.jupiter.api.Assertions.assertTrue(fixture.attempt().getFailureReason().contains("reconciliation"));
        verify(recovery).recordCaptureReconciliation(fixture.order(), fixture.attempt(), fixture.attempt().getFailureReason());
        verify(recovery, never()).markRecovered(any());
    }

    @Test
    void lateFailedEventCannotChangeCancelledAttemptBackToFailed() throws Exception {
        Fixture fixture = fixture(OrderStatus.CANCELLED, PaymentStatus.CANCELLED, PaymentStatus.CANCELLED,
                "order_123", null);
        String body = "{\"event\":\"payment.failed\",\"payload\":{\"payment\":{\"entity\":{\"id\":\"pay_late_fail\",\"order_id\":\"order_123\",\"error_description\":\"late failure\"}}}}";
        when(events.existsByEventId("evt_late_fail")).thenReturn(false);
        stubFixture(fixture, "order_123");

        controller.receive(body, signature(body), "evt_late_fail");

        assertEquals(OrderStatus.CANCELLED, fixture.order().getStatus());
        assertEquals(PaymentStatus.CANCELLED, fixture.payment().getPaymentStatus());
        assertEquals(PaymentStatus.CANCELLED, fixture.attempt().getStatus());
        verify(recovery, never()).detectFailedPayment(any());
    }

    private Fixture fixture(OrderStatus orderStatus, PaymentStatus paymentStatus,
                            PaymentStatus attemptStatus, String razorpayOrderId, String priorPaymentId) {
        Order order = Order.builder().status(orderStatus).build();
        order.setId(123L);
        Payment payment = Payment.builder().id(456L).order(order).amount(new BigDecimal("125.00"))
                .paymentStatus(paymentStatus).razorpayOrderId(razorpayOrderId)
                .razorpayPaymentId(priorPaymentId).build();
        PaymentAttempt attempt = PaymentAttempt.builder().id(789L).payment(payment).attemptNumber(1)
                .amount(payment.getAmount()).status(attemptStatus).razorpayOrderId(razorpayOrderId)
                .razorpayPaymentId(priorPaymentId).build();
        return new Fixture(order, payment, attempt);
    }

    private void stubFixture(Fixture fixture, String razorpayOrderId) {
        when(paymentAttempts.findByRazorpayOrderId(razorpayOrderId)).thenReturn(Optional.of(fixture.attempt()));
        when(paymentAttempts.findByRazorpayOrderIdForUpdate(razorpayOrderId)).thenReturn(Optional.of(fixture.attempt()));
        when(paymentAttempts.findByRazorpayPaymentId(any())).thenReturn(Optional.empty());
        when(payments.findByRazorpayPaymentId(any())).thenReturn(Optional.empty());
        when(orders.findByIdForUpdate(123L)).thenReturn(Optional.of(fixture.order()));
        when(payments.findByOrderIdForUpdate(123L)).thenReturn(Optional.of(fixture.payment()));
        when(payments.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentAttempts.save(any(PaymentAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orders.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentAttempts.findByPaymentIdAndStatusOrderByAttemptNumberDesc(456L, PaymentStatus.PENDING))
                .thenReturn(java.util.List.of());
    }

    private String captured(String paymentId, String orderId) {
        return "{\"event\":\"payment.captured\",\"payload\":{\"payment\":{\"entity\":{\"id\":\""
                + paymentId + "\",\"order_id\":\"" + orderId + "\"}}}}";
    }

    private record Fixture(Order order, Payment payment, PaymentAttempt attempt) { }

    private static String signature(String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    }
}
