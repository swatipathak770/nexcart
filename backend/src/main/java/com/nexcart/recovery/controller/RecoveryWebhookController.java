package com.nexcart.recovery.controller;

import com.nexcart.entity.OrderStatus;
import com.nexcart.entity.Order;
import com.nexcart.entity.Payment;
import com.nexcart.entity.PaymentAttempt;
import com.nexcart.entity.PaymentStatus;
import com.nexcart.recovery.entity.ProcessedWebhookEvent;
import com.nexcart.recovery.repository.ProcessedWebhookEventRepository;
import com.nexcart.recovery.service.RecoveryService;
import com.nexcart.repository.OrderRepository;
import com.nexcart.repository.PaymentRepository;
import com.nexcart.repository.PaymentAttemptRepository;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
public class RecoveryWebhookController {
    private final PaymentRepository payments;
    private final PaymentAttemptRepository paymentAttempts;
    private final OrderRepository orders;
    private final RecoveryService recovery;
    private final ProcessedWebhookEventRepository processedEvents;

    @Value("${razorpay.webhook-secret:}")
    private String webhookSecret;

    @PostMapping(value = "/razorpay", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<Map<String, String>> receive(
            @RequestBody String body,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
            @RequestHeader(value = "X-Razorpay-Event-Id", required = false) String suppliedEventId
    ) throws Exception {
        if (webhookSecret.isBlank() || signature == null
                || !Utils.verifyWebhookSignature(body, signature, webhookSecret)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid webhook signature"));
        }

        JSONObject root = new JSONObject(body);
        String eventType = root.optString("event", "unknown");
        String eventId = suppliedEventId == null || suppliedEventId.isBlank()
                ? "sha256:" + sha256(body)
                : suppliedEventId.trim();
        if (processedEvents.existsByEventId(eventId)) {
            log.info("Duplicate Razorpay webhook ignored; eventType={}", eventType);
            return ResponseEntity.ok(Map.of("message", "Duplicate event ignored"));
        }

        processEvent(root, eventType);

        ProcessedWebhookEvent processed = new ProcessedWebhookEvent();
        processed.setEventId(eventId);
        processed.setEventType(eventType);
        processed.setProcessedAt(LocalDateTime.now());
        processedEvents.save(processed);
        log.info("Razorpay webhook processed; eventType={}", eventType);
        return ResponseEntity.ok(Map.of("message", "Processed"));
    }

    private void processEvent(JSONObject root, String eventType) {
        JSONObject payload = root.optJSONObject("payload");
        if (payload == null) return;

        if ("payment_link.paid".equals(eventType)) {
            JSONObject linkPayload = payload.optJSONObject("payment_link");
            JSONObject paymentPayload = payload.optJSONObject("payment");
            JSONObject link = linkPayload == null ? null : linkPayload.optJSONObject("entity");
            JSONObject payment = paymentPayload == null ? null : paymentPayload.optJSONObject("entity");
            if (link != null) {
                recovery.handlePaymentLinkPaid(
                        link.optString("id"),
                        payment == null ? null : payment.optString("id", null),
                        link.optString("reference_id", null));
            }
            return;
        }

        JSONObject paymentPayload = payload.optJSONObject("payment");
        JSONObject entity = paymentPayload == null ? null : paymentPayload.optJSONObject("entity");
        if (entity == null) return;

        String razorpayOrderId = entity.optString("order_id", null);
        if (razorpayOrderId == null || razorpayOrderId.isBlank()) return;
        PaymentAttempt observedAttempt = paymentAttempts.findByRazorpayOrderId(razorpayOrderId).orElse(null);
        Payment observedPayment = observedAttempt == null
                ? payments.findByRazorpayOrderId(razorpayOrderId).orElse(null)
                : observedAttempt.getPayment();
        if (observedPayment == null) return;

        // Lock order -> payment -> attempt, matching customer cancellation and
        // verification. Re-read state only after acquiring those locks.
        Long orderId = observedPayment.getOrder().getId();
        Order order = orders.findByIdForUpdate(orderId).orElse(null);
        if (order == null) return;
        Payment payment = payments.findByOrderIdForUpdate(orderId).orElse(null);
        if (payment == null) return;
        PaymentAttempt attempt = paymentAttempts.findByRazorpayOrderIdForUpdate(razorpayOrderId).orElse(null);
        String paymentId = entity.optString("id", null);

        if ("payment.failed".equals(eventType)) {
            if (attempt == null || attempt.getStatus() != PaymentStatus.PENDING) return;
            attempt.setStatus(PaymentStatus.FAILED);
            attempt.setRazorpayPaymentId(paymentId);
            attempt.setFailureReason(entity.optString("error_description", "Razorpay payment failed"));
            paymentAttempts.save(attempt);
            if (razorpayOrderId.equals(payment.getRazorpayOrderId())
                    && payment.getPaymentStatus() == PaymentStatus.PENDING
                    && order.getStatus() == OrderStatus.PENDING) {
                payment.setPaymentStatus(PaymentStatus.FAILED);
                payment.setFailureReason(attempt.getFailureReason());
                Payment failed = payments.save(payment);
                recovery.recordRetryAttemptEnded(failed, PaymentStatus.FAILED);
                recovery.detectFailedPayment(failed);
            }
            return;
        }

        if (!"payment.captured".equals(eventType) || paymentId == null || paymentId.isBlank()) return;
        if (attempt != null && attempt.getStatus() == PaymentStatus.SUCCESS
                && paymentId.equals(attempt.getRazorpayPaymentId())) return;

        PaymentAttempt paymentIdOwner = paymentAttempts.findByRazorpayPaymentId(paymentId).orElse(null);
        if (paymentIdOwner != null && (attempt == null || !paymentIdOwner.getId().equals(attempt.getId()))) {
            flagForReconciliation(order, payment, attempt,
                    "Razorpay reported a capture ID already recorded on another attempt: " + paymentId);
            return;
        }
        if (paymentRepositoryCaptureAlreadyRecorded(paymentId)) {
            flagForReconciliation(order, payment, attempt,
                    "Razorpay reported a capture ID already recorded on the payment: " + paymentId);
            return;
        }
        if (attempt != null && attempt.getStatus() == PaymentStatus.SUCCESS) {
            flagForReconciliation(order, payment, attempt,
                    "A second capture was reported for this attempt (" + paymentId + "); refund/manual reconciliation required; no refund has been recorded.");
            return;
        }

        if (attempt != null) {
            attempt.setStatus(PaymentStatus.SUCCESS);
            attempt.setRazorpayPaymentId(paymentId);
            attempt.setFailureReason(null);
            paymentAttempts.save(attempt);
        }

        boolean orderCancelled = order.getStatus() == OrderStatus.CANCELLED;
        boolean paymentAlreadyCaptured = payment.getPaymentStatus() == PaymentStatus.SUCCESS;
        boolean anotherCapture = paymentAlreadyCaptured && !paymentId.equals(payment.getRazorpayPaymentId());
        if (orderCancelled || anotherCapture || order.getStatus() != OrderStatus.PENDING) {
            if (!paymentAlreadyCaptured) {
                payment.setPaymentStatus(PaymentStatus.SUCCESS);
                payment.setRazorpayOrderId(razorpayOrderId);
                payment.setRazorpayPaymentId(paymentId);
                payment.setPaidAt(LocalDateTime.now());
            }
            String reason = orderCancelled
                    ? "Payment captured after order cancellation; refund/manual reconciliation required; no refund has been recorded. Razorpay payment ID: " + paymentId
                    : "Additional or late capture requires refund/manual reconciliation; no refund has been recorded. Razorpay payment ID: " + paymentId;
            flagForReconciliation(order, payment, attempt, reason);
            return;
        }

        payment.setPaymentStatus(PaymentStatus.SUCCESS);
        payment.setRazorpayOrderId(razorpayOrderId);
        payment.setRazorpayPaymentId(paymentId);
        payment.setPaidAt(LocalDateTime.now());
        payment.setFailureReason(null);
        if (attempt == null) {
            paymentAttempts.findByRazorpayOrderIdForUpdate(razorpayOrderId).ifPresent(legacyAttempt -> {
                legacyAttempt.setStatus(PaymentStatus.SUCCESS);
                legacyAttempt.setRazorpayPaymentId(paymentId);
                paymentAttempts.save(legacyAttempt);
            });
        }
        paymentAttempts.findByPaymentIdAndStatusOrderByAttemptNumberDesc(payment.getId(), PaymentStatus.PENDING)
                .forEach(other -> {
                    if (attempt == null || !other.getId().equals(attempt.getId())) {
                        other.setStatus(PaymentStatus.CANCELLED);
                        other.setFailureReason("Superseded by a verified successful payment attempt.");
                        paymentAttempts.save(other);
                    }
                });
        order.setStatus(OrderStatus.CONFIRMED);
        orders.save(order);
        recovery.markRecovered(payments.save(payment));
    }

    private boolean paymentRepositoryCaptureAlreadyRecorded(String paymentId) {
        return payments.findByRazorpayPaymentId(paymentId).isPresent();
    }

    private void flagForReconciliation(Order order, Payment payment, PaymentAttempt attempt, String reason) {
        if (attempt != null) {
            attempt.setFailureReason(reason);
            paymentAttempts.save(attempt);
        }
        payment.setFailureReason(reason);
        payments.save(payment);
        recovery.recordCaptureReconciliation(order, attempt, reason);
        log.error("Razorpay capture requires manual reconciliation; orderId={} attemptId={}",
                order.getId(), attempt == null ? null : attempt.getId());
    }

    private String sha256(String body) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(body.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest);
    }
}
