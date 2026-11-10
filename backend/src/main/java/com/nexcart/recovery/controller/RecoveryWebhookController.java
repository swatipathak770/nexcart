package com.nexcart.recovery.controller;

import com.nexcart.entity.OrderStatus;
import com.nexcart.entity.PaymentStatus;
import com.nexcart.recovery.entity.ProcessedWebhookEvent;
import com.nexcart.recovery.repository.ProcessedWebhookEventRepository;
import com.nexcart.recovery.service.RecoveryService;
import com.nexcart.repository.OrderRepository;
import com.nexcart.repository.PaymentRepository;
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
        payments.findByRazorpayOrderIdForUpdate(razorpayOrderId).ifPresent(payment -> {
            if ("payment.failed".equals(eventType)
                    && payment.getPaymentStatus() != PaymentStatus.SUCCESS) {
                payment.setPaymentStatus(PaymentStatus.FAILED);
                payment.setFailureReason(entity.optString("error_description", "Razorpay payment failed"));
                recovery.detectFailedPayment(payments.save(payment));
            } else if ("payment.captured".equals(eventType)
                    && payment.getPaymentStatus() != PaymentStatus.SUCCESS) {
                payment.setPaymentStatus(PaymentStatus.SUCCESS);
                payment.setRazorpayPaymentId(entity.optString("id", null));
                payment.setPaidAt(LocalDateTime.now());
                payment.getOrder().setStatus(OrderStatus.CONFIRMED);
                orders.save(payment.getOrder());
                recovery.markRecovered(payments.save(payment));
            }
        });
    }

    private String sha256(String body) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(body.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest);
    }
}
