package com.nexcart.service;

import com.nexcart.dto.request.PaymentRequest;
import com.nexcart.dto.response.PaymentResponse;
import com.nexcart.dto.request.PaymentVerificationRequest;

public interface PaymentService {

    PaymentResponse createPaymentOrder(PaymentRequest request) throws Exception;

    PaymentResponse verifyPayment(PaymentVerificationRequest request) throws Exception;

    PaymentResponse getPaymentByOrderId(Long orderId);

    PaymentResponse getPaymentById(Long paymentId);
}
