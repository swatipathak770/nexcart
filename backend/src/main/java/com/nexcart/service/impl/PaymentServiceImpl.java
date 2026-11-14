package com.nexcart.service.impl;

import com.nexcart.dto.request.PaymentFailureRequest;
import com.nexcart.dto.request.PaymentRequest;
import com.nexcart.dto.request.PaymentVerificationRequest;
import com.nexcart.dto.response.PaymentResponse;
import com.nexcart.dto.response.PaymentCheckoutResponse;
import com.nexcart.entity.Order;
import com.nexcart.entity.OrderStatus;
import com.nexcart.entity.Payment;
import com.nexcart.entity.PaymentAttempt;
import com.nexcart.entity.PaymentMethod;
import com.nexcart.entity.PaymentStatus;
import com.nexcart.exception.ResourceNotFoundException;
import com.nexcart.mapper.PaymentMapper;
import com.nexcart.repository.OrderRepository;
import com.nexcart.repository.PaymentRepository;
import com.nexcart.repository.PaymentAttemptRepository;
import com.nexcart.service.PaymentService;
import com.nexcart.recovery.service.RecoveryService;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final OrderRepository orderRepository;
    private final RazorpayClient razorpayClient;
    private final RecoveryService recoveryService;

    @Value("${razorpay.key-secret}")
    private String razorpayKeySecret;

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    @Override
    public PaymentResponse createPaymentOrder(PaymentRequest request) throws Exception {
        Order order = findOwnedOrderForUpdate(request.getOrderId());
        if (order.getStatus() != OrderStatus.PENDING) throw new IllegalStateException("Payment cannot be started for this order.");

        Payment existing = paymentRepository.findByOrderIdForUpdate(order.getId()).orElse(null);
        if (existing != null && existing.getPaymentStatus() == PaymentStatus.SUCCESS) throw new IllegalStateException("This order has already been paid.");
        if (existing != null && existing.getPaymentStatus() == PaymentStatus.PENDING) return PaymentMapper.toResponse(existing);

        JSONObject options = new JSONObject();
        options.put("amount", order.getFinalAmount().multiply(BigDecimal.valueOf(100)).longValueExact());
        options.put("currency", "INR");
        options.put("receipt", "NEXCART_" + order.getId());
        com.razorpay.Order razorpayOrder = razorpayClient.orders.create(options);

        Payment payment = existing == null ? Payment.builder().order(order).amount(order.getFinalAmount()).transactionId(UUID.randomUUID().toString()).build() : existing;
        payment.setAmount(order.getFinalAmount());
        payment.setPaymentStatus(PaymentStatus.PENDING);
        payment.setRazorpayOrderId(razorpayOrder.get("id"));
        payment.setRazorpayPaymentId(null);
        payment.setRazorpaySignature(null);
        payment.setFailureReason(null);
        payment.setPaidAt(null);
        Payment initiated = paymentRepository.save(payment);
        int attemptNumber = paymentAttemptRepository.findByPaymentIdOrderByAttemptNumberAsc(initiated.getId()).stream()
                .mapToInt(PaymentAttempt::getAttemptNumber).max().orElse(0) + 1;
        paymentAttemptRepository.save(PaymentAttempt.builder().payment(initiated).attemptNumber(attemptNumber)
                .amount(initiated.getAmount()).status(PaymentStatus.PENDING)
                .razorpayOrderId(initiated.getRazorpayOrderId()).idempotencyKey("checkout-" + initiated.getId() + "-" + attemptNumber).build());
        log.info("Payment order initiated for orderId={}", order.getId());
        return PaymentMapper.toResponse(initiated);
    }

    @Override
    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public PaymentResponse verifyPayment(PaymentVerificationRequest request) throws Exception {
        Order order = findOwnedOrderForUpdate(request.getOrderId());
        Payment payment = paymentRepository.findByOrderIdForUpdate(order.getId()).orElseThrow(() -> new ResourceNotFoundException("Payment was not initiated for this order."));
        if (payment.getPaymentStatus() == PaymentStatus.SUCCESS) return PaymentMapper.toResponse(payment);
        if (payment.getPaymentStatus() != PaymentStatus.PENDING) throw new IllegalStateException("This payment attempt is no longer pending.");
        PaymentAttempt attempt = paymentAttemptRepository.findByRazorpayOrderIdForUpdate(payment.getRazorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment attempt was not found."));
        if (attempt.getStatus() == PaymentStatus.SUCCESS) return PaymentMapper.toResponse(payment);
        if (attempt.getStatus() != PaymentStatus.PENDING) throw new IllegalStateException("This payment attempt is no longer pending.");
        if (paymentRepository.findByRazorpayPaymentId(request.getRazorpayPaymentId()).isPresent()
                || paymentAttemptRepository.findByRazorpayPaymentId(request.getRazorpayPaymentId()).isPresent())
            throw new IllegalArgumentException("This Razorpay payment has already been recorded.");

        JSONObject verificationData = new JSONObject();
        verificationData.put("razorpay_order_id", payment.getRazorpayOrderId());
        verificationData.put("razorpay_payment_id", request.getRazorpayPaymentId());
        verificationData.put("razorpay_signature", request.getRazorpaySignature());
        if (!Utils.verifyPaymentSignature(verificationData, razorpayKeySecret.trim())) {
            payment.setPaymentStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Razorpay signature verification failed.");
            attempt.setStatus(PaymentStatus.FAILED);
            attempt.setFailureReason(payment.getFailureReason());
            paymentAttemptRepository.save(attempt);
            Payment failed = paymentRepository.save(payment);
            recoveryService.detectFailedPayment(failed);
            recoveryService.recordRetryAttemptEnded(failed, PaymentStatus.FAILED);
            log.warn("Payment signature verification failed for orderId={}", order.getId());
            throw new IllegalArgumentException("Payment verification failed.");
        }

        payment.setPaymentStatus(PaymentStatus.SUCCESS);
        payment.setPaymentMethod(PaymentMethod.UPI);
        payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
        payment.setRazorpaySignature(request.getRazorpaySignature());
        payment.setPaidAt(LocalDateTime.now());
        attempt.setStatus(PaymentStatus.SUCCESS);
        attempt.setRazorpayPaymentId(request.getRazorpayPaymentId());
        attempt.setRazorpaySignature(request.getRazorpaySignature());
        attempt.setFailureReason(null);
        paymentAttemptRepository.save(attempt);
        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);
        Payment saved = paymentRepository.save(payment);
        recoveryService.markRecovered(saved);
        log.info("Payment verified for orderId={}", order.getId());
        return PaymentMapper.toResponse(saved);
    }

    @Override
    public PaymentResponse markPaymentFailed(PaymentFailureRequest request) { return updatePendingPaymentStatus(request, PaymentStatus.FAILED, "Payment failed in Razorpay Checkout."); }

    @Override
    public PaymentResponse cancelPayment(PaymentFailureRequest request) { return updatePendingPaymentStatus(request, PaymentStatus.CANCELLED, "Payment cancelled by customer."); }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId).orElseThrow(() -> new ResourceNotFoundException("Payment not found for order id: " + orderId));
        assertCurrentUserOwns(payment.getOrder());
        return PaymentMapper.toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId).orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));
        assertCurrentUserOwns(payment.getOrder());
        return PaymentMapper.toResponse(payment);
    }

    @Override
    public PaymentCheckoutResponse getRetryCheckout(Long orderId) {
        Order order = findOwnedOrderForUpdate(orderId);
        paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment was not initiated for this order."));
        if (!recoveryService.retryCheckoutAvailable(order)) throw new IllegalStateException("A payment retry is not ready for this order yet.");
        PaymentAttempt attempt;
        try {
            attempt = recoveryService.prepareRetryAttemptForOrder(order);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to prepare a secure payment checkout. Please try again.", ex);
        }
        return new PaymentCheckoutResponse(orderId, attempt.getRazorpayOrderId(), razorpayKeyId,
                attempt.getAmount(), "INR");
    }

    private PaymentResponse updatePendingPaymentStatus(PaymentFailureRequest request, PaymentStatus status, String defaultReason) {
        Order order = findOwnedOrderForUpdate(request.getOrderId());
        Payment payment = paymentRepository.findByOrderIdForUpdate(order.getId()).orElseThrow(() -> new ResourceNotFoundException("Payment was not initiated for this order."));
        if (payment.getPaymentStatus() == PaymentStatus.SUCCESS) return PaymentMapper.toResponse(payment);
        if (payment.getPaymentStatus() == PaymentStatus.PENDING) {
            payment.setPaymentStatus(status);
            payment.setFailureReason(request.getReason() == null || request.getReason().isBlank() ? defaultReason : request.getReason());
            paymentAttemptRepository.findByRazorpayOrderId(payment.getRazorpayOrderId()).ifPresent(attempt -> {
                attempt.setStatus(status);
                attempt.setFailureReason(payment.getFailureReason());
                paymentAttemptRepository.save(attempt);
            });
            Payment updated = paymentRepository.save(payment);
            if (status == PaymentStatus.FAILED) recoveryService.detectFailedPayment(updated);
            recoveryService.recordRetryAttemptEnded(updated, status);
        }
        return PaymentMapper.toResponse(payment);
    }

    private Order findOwnedOrder(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        assertCurrentUserOwns(order);
        return order;
    }

    private Order findOwnedOrderForUpdate(Long orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        assertCurrentUserOwns(order);
        return order;
    }

    private void assertCurrentUserOwns(Order order) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!order.getUser().getEmail().equals(email)) throw new AccessDeniedException("You are not authorized to access this payment.");
    }
}
