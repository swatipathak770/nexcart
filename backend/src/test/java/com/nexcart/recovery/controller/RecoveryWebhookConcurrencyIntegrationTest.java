package com.nexcart.recovery.controller;

import com.nexcart.entity.*;
import com.nexcart.recovery.entity.RecoveryAuditLog;
import com.nexcart.recovery.entity.RecoveryCase;
import com.nexcart.recovery.enums.RecoveryStatus;
import com.nexcart.recovery.enums.RecoveryType;
import com.nexcart.recovery.repository.RecoveryAuditRepository;
import com.nexcart.recovery.repository.RecoveryCaseRepository;
import com.nexcart.repository.*;
import com.nexcart.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class RecoveryWebhookConcurrencyIntegrationTest {
    @DynamicPropertySource
    static void forceIsolatedInMemoryDatabase(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> "jdbc:h2:mem:webhook-concurrency-test;MODE=MySQL;DB_CLOSE_DELAY=-1");
        properties.add("spring.datasource.username", () -> "sa");
        properties.add("spring.datasource.password", () -> "");
        properties.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        properties.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        properties.add("spring.flyway.enabled", () -> "false");
    }

    @Autowired private RecoveryWebhookController webhook;
    @Autowired private OrderService orderService;
    @Autowired private UserRepository users;
    @Autowired private CategoryRepository categories;
    @Autowired private BrandRepository brands;
    @Autowired private ProductRepository products;
    @Autowired private OrderRepository orders;
    @Autowired private PaymentRepository payments;
    @Autowired private PaymentAttemptRepository attempts;
    @Autowired private RecoveryCaseRepository recoveryCases;
    @Autowired private RecoveryAuditRepository recoveryAudits;
    @Autowired private PlatformTransactionManager transactionManager;
    @Value("${razorpay.webhook-secret}") private String webhookSecret;

    @Test
    void concurrentCustomerCancellationAndCaptureLeaveEitherConfirmedOrCancelledForReconciliation() throws Exception {
        Fixture f = fixture(true);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var cancellation = pool.submit(() -> {
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(f.email(), null));
                ready.countDown();
                try {
                    if (!start.await(10, java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("barrier timed out");
                    orderService.cancelOrder(f.orderId());
                    return "CANCELLED";
                } catch (IllegalStateException alreadyPaid) {
                    return "CANCEL_REJECTED_AFTER_CAPTURE";
                } finally { SecurityContextHolder.clearContext(); }
            });
            var capture = pool.submit(() -> {
                ready.countDown();
                if (!start.await(10, java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("barrier timed out");
                String body = captured(f.razorpayOrderId(), f.razorpayPaymentId());
                return webhook.receive(body, signature(body), "evt-race-" + UUID.randomUUID()).getStatusCode().value();
            });
            assertTrue(ready.await(10, java.util.concurrent.TimeUnit.SECONDS));
            start.countDown();
            cancellation.get(30, java.util.concurrent.TimeUnit.SECONDS);
            assertEquals(200, capture.get(30, java.util.concurrent.TimeUnit.SECONDS));
        } finally {
            start.countDown();
            pool.shutdownNow();
            SecurityContextHolder.clearContext();
        }

        Order order = orders.findById(f.orderId()).orElseThrow();
        Payment payment = payments.findByOrderId(f.orderId()).orElseThrow();
        PaymentAttempt attempt = attempts.findByRazorpayOrderId(f.razorpayOrderId()).orElseThrow();
        assertEquals(PaymentStatus.SUCCESS, payment.getPaymentStatus());
        assertEquals(PaymentStatus.SUCCESS, attempt.getStatus());
        if (order.getStatus() == OrderStatus.CANCELLED) {
            assertTrue(payment.getFailureReason().contains("manual reconciliation"));
            assertTrue(products.findById(f.productId()).orElseThrow().getStock() == 5);
            assertTrue(recoveryAudits.findByRecoveryCaseIdOrderByCreatedAtAsc(f.recoveryCaseId()).stream()
                    .anyMatch(a -> a.getEventType().equals("CAPTURE_REQUIRES_REFUND_RECONCILIATION")));
        } else {
            assertEquals(OrderStatus.CONFIRMED, order.getStatus());
            assertEquals(4, products.findById(f.productId()).orElseThrow().getStock());
        }
    }

    @Test
    void multipleCapturesForDifferentAttemptsKeepOnePrimaryPaymentAndFlagSecondCapture() throws Exception {
        Fixture f = fixture(false);
        PaymentAttempt second = new TransactionTemplate(transactionManager).execute(status -> attempts.save(
                PaymentAttempt.builder().payment(payments.findByOrderId(f.orderId()).orElseThrow()).attemptNumber(2)
                        .amount(new BigDecimal("10.00")).status(PaymentStatus.PENDING)
                        .razorpayOrderId(f.razorpayOrderId() + "_second").build()));

        String firstBody = captured(f.razorpayOrderId(), f.razorpayPaymentId());
        webhook.receive(firstBody, signature(firstBody), "evt-first-" + UUID.randomUUID());
        String secondBody = captured(f.razorpayOrderId() + "_second", f.razorpayPaymentId() + "_second");
        webhook.receive(secondBody, signature(secondBody), "evt-second-" + UUID.randomUUID());

        Payment payment = payments.findByOrderId(f.orderId()).orElseThrow();
        Order order = orders.findById(f.orderId()).orElseThrow();
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        assertEquals(f.razorpayPaymentId(), payment.getRazorpayPaymentId());
        assertTrue(payments.existsByOrderId(f.orderId()));
        assertEquals(PaymentStatus.SUCCESS, attempts.findById(second.getId()).orElseThrow().getStatus());
        assertTrue(attempts.findById(second.getId()).orElseThrow().getFailureReason().contains("reconciliation"));
    }

    private Fixture fixture(boolean includeRecoveryCase) {
        String suffix = UUID.randomUUID().toString();
        return new TransactionTemplate(transactionManager).execute(status -> {
            User user = users.save(User.builder().fullName("Webhook Test").email("webhook-" + suffix + "@example.test")
                    .password("test-only").phoneNumber("7" + suffix.replaceAll("\\D", "").substring(0, 9))
                    .role(Role.CUSTOMER).enabled(true).accountNonLocked(true).accountNonExpired(true)
                    .credentialsNonExpired(true).build());
            Category category = categories.save(Category.builder().name("Webhook category " + suffix).build());
            Brand brand = brands.save(Brand.builder().name("Webhook brand " + suffix).build());
            Product product = products.save(Product.builder().name("Webhook product " + suffix).sku("WH-" + suffix)
                    .price(new BigDecimal("10.00")).stock(4).active(true).category(category).brand(brand).build());
            Order order = Order.builder().user(user).status(OrderStatus.PENDING).totalAmount(new BigDecimal("10.00"))
                    .discountAmount(BigDecimal.ZERO).finalAmount(new BigDecimal("10.00"))
                    .orderItems(new java.util.ArrayList<>()).build();
            order.getOrderItems().add(OrderItem.builder().order(order).product(product).quantity(1)
                    .price(new BigDecimal("10.00")).build());
            order = orders.save(order);
            String razorpayOrderId = "order_" + suffix.replace("-", "");
            Payment payment = payments.save(Payment.builder().order(order).amount(new BigDecimal("10.00"))
                    .paymentStatus(PaymentStatus.PENDING).razorpayOrderId(razorpayOrderId).build());
            String paymentId = "pay_" + suffix.replace("-", "");
            attempts.save(PaymentAttempt.builder().payment(payment).attemptNumber(1)
                    .amount(payment.getAmount()).status(PaymentStatus.PENDING).razorpayOrderId(razorpayOrderId).build());
            Long caseId = null;
            if (includeRecoveryCase) {
                RecoveryCase recovery = recoveryCases.save(RecoveryCase.builder().order(order).user(user)
                        .amount(payment.getAmount()).type(RecoveryType.PAYMENT_FAILED).failureReason("Test recovery")
                        .status(RecoveryStatus.ACTION_PENDING).build());
                caseId = recovery.getId();
            }
            return new Fixture(order.getId(), user.getEmail(), product.getId(), caseId, razorpayOrderId, paymentId);
        });
    }

    private String captured(String orderId, String paymentId) {
        return "{\"event\":\"payment.captured\",\"payload\":{\"payment\":{\"entity\":{\"id\":\""
                + paymentId + "\",\"order_id\":\"" + orderId + "\"}}}}";
    }

    private String signature(String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    }

    private record Fixture(Long orderId, String email, Long productId, Long recoveryCaseId,
                           String razorpayOrderId, String razorpayPaymentId) { }
}
