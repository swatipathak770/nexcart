package com.nexcart.service.impl;

import com.nexcart.dto.request.PaymentRequest;
import com.nexcart.dto.response.PaymentResponse;
import com.nexcart.dto.request.PaymentVerificationRequest;
import com.nexcart.entity.Order;
import com.nexcart.entity.Payment;
import com.nexcart.entity.PaymentStatus;
import com.nexcart.exception.ResourceNotFoundException;
import com.nexcart.mapper.PaymentMapper;
import com.nexcart.repository.OrderRepository;
import com.nexcart.repository.PaymentRepository;
import com.nexcart.service.PaymentService;
import com.razorpay.RazorpayClient;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final RazorpayClient razorpayClient;

    @Override
    public PaymentResponse createPaymentOrder(PaymentRequest request) throws Exception {

        // Fetch Order
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order not found with id: "
                                + request.getOrderId()));

        // Prevent duplicate payment
        if (paymentRepository.existsByOrderId(order.getId())) {
            throw new IllegalStateException(
                    "Payment already exists for this order.");
        }

        // Razorpay expects amount in paise
        JSONObject options = new JSONObject();

        options.put("amount",
                order.getFinalAmount()
                        .multiply(BigDecimal.valueOf(100))
                        .longValue());

        options.put("currency", "INR");

        options.put("receipt", "ORDER_" + order.getId());

        // Create Razorpay Order
        com.razorpay.Order razorpayOrder =
                razorpayClient.orders.create(options);

        // Save Payment
        Payment payment = Payment.builder()
                .order(order)
                .amount(order.getFinalAmount())
                .paymentStatus(PaymentStatus.PENDING)
                .razorpayOrderId(razorpayOrder.get("id"))
                .transactionId(UUID.randomUUID().toString())
                .build();

        paymentRepository.save(payment);

        return PaymentMapper.toResponse(payment);
    }

    @Override
    public PaymentResponse verifyPayment(PaymentVerificationRequest request) throws Exception {
        throw new UnsupportedOperationException("Will implement next.");
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(Long orderId) {

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Payment not found for order id: " + orderId));

        return PaymentMapper.toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long paymentId) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Payment not found with id: " + paymentId));

        return PaymentMapper.toResponse(payment);
    }
}
