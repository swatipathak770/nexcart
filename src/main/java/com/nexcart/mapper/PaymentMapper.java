package com.nexcart.mapper;

import com.nexcart.dto.response.PaymentResponse;
import com.nexcart.entity.Payment;

public class PaymentMapper {

    private PaymentMapper() {
    }

    public static PaymentResponse toResponse(Payment payment) {

        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrder().getId())
                .amount(payment.getAmount())
                .paymentStatus(payment.getPaymentStatus())
                .paymentMethod(payment.getPaymentMethod())
                .transactionId(payment.getTransactionId())
                .razorpayOrderId(payment.getRazorpayOrderId())
                .razorpayPaymentId(payment.getRazorpayPaymentId())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
