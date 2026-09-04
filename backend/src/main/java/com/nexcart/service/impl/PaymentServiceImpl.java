package com.nexcart.service.impl;

import com.nexcart.dto.request.PaymentFailureRequest;
import com.nexcart.dto.request.PaymentRequest;
import com.nexcart.dto.request.PaymentVerificationRequest;
import com.nexcart.dto.response.PaymentResponse;
import com.nexcart.entity.Order;
import com.nexcart.entity.OrderStatus;
import com.nexcart.entity.Payment;
import com.nexcart.entity.PaymentMethod;
import com.nexcart.entity.PaymentStatus;
import com.nexcart.exception.ResourceNotFoundException;
import com.nexcart.mapper.PaymentMapper;
import com.nexcart.repository.OrderRepository;
import com.nexcart.repository.PaymentRepository;
import com.nexcart.service.PaymentService;
import com.nexcart.recovery.service.RecoveryService;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final RazorpayClient razorpayClient;
    private final RecoveryService recoveryService;

    @Value("${razorpay.key-secret}")
    private String razorpayKeySecret;

    @Override
    public PaymentResponse createPaymentOrder(PaymentRequest request) throws Exception {
        Order order = findOwnedOrder(request.getOrderId());
        if (order.getStatus() != OrderStatus.PENDING) throw new IllegalStateException("Payment cannot be started for this order.");

        Payment existing = paymentRepository.findByOrderId(order.getId()).orElse(null);
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
        return PaymentMapper.toResponse(paymentRepository.save(payment));
    }

    @Override
    public PaymentResponse verifyPayment(PaymentVerificationRequest request) throws Exception {
        Order order = findOwnedOrder(request.getOrderId());
        Payment payment = paymentRepository.findByOrderId(order.getId()).orElseThrow(() -> new ResourceNotFoundException("Payment was not initiated for this order."));
        if (payment.getPaymentStatus() == PaymentStatus.SUCCESS) return PaymentMapper.toResponse(payment);
        if (payment.getPaymentStatus() != PaymentStatus.PENDING) throw new IllegalStateException("This payment attempt is no longer pending.");
        if (paymentRepository.findByRazorpayPaymentId(request.getRazorpayPaymentId()).isPresent()) throw new IllegalArgumentException("This Razorpay payment has already been recorded.");

        JSONObject verificationData = new JSONObject();
        verificationData.put("razorpay_order_id", payment.getRazorpayOrderId());
        verificationData.put("razorpay_payment_id", request.getRazorpayPaymentId());
        verificationData.put("razorpay_signature", request.getRazorpaySignature());
        if (!Utils.verifyPaymentSignature(verificationData, razorpayKeySecret.trim())) {
            payment.setPaymentStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Razorpay signature verification failed.");
            Payment failed = paymentRepository.save(payment);
            recoveryService.detectFailedPayment(failed);
            throw new IllegalArgumentException("Payment verification failed.");
        }

        payment.setPaymentStatus(PaymentStatus.SUCCESS);
        payment.setPaymentMethod(PaymentMethod.UPI);
        payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
        payment.setRazorpaySignature(request.getRazorpaySignature());
        payment.setPaidAt(LocalDateTime.now());
        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);
        Payment saved = paymentRepository.save(payment);
        recoveryService.markRecovered(saved);
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

    private PaymentResponse updatePendingPaymentStatus(PaymentFailureRequest request, PaymentStatus status, String defaultReason) {
        Order order = findOwnedOrder(request.getOrderId());
        Payment payment = paymentRepository.findByOrderId(order.getId()).orElseThrow(() -> new ResourceNotFoundException("Payment was not initiated for this order."));
        if (payment.getPaymentStatus() == PaymentStatus.SUCCESS) return PaymentMapper.toResponse(payment);
        if (payment.getPaymentStatus() == PaymentStatus.PENDING) {
            payment.setPaymentStatus(status);
            payment.setFailureReason(request.getReason() == null || request.getReason().isBlank() ? defaultReason : request.getReason());
            Payment updated = paymentRepository.save(payment);
            if (status == PaymentStatus.FAILED) recoveryService.detectFailedPayment(updated);
        }
        return PaymentMapper.toResponse(payment);
    }

    private Order findOwnedOrder(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        assertCurrentUserOwns(order);
        return order;
    }

    private void assertCurrentUserOwns(Order order) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!order.getUser().getEmail().equals(email)) throw new IllegalStateException("You are not authorized to access this payment.");
    }
}
