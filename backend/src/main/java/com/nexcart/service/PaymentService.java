package com.nexcart.service;

import com.nexcart.dto.request.PaymentRequest;
import com.nexcart.dto.response.PaymentResponse;
import com.nexcart.dto.request.PaymentVerificationRequest;
import com.nexcart.dto.request.PaymentFailureRequest;

public interface PaymentService {

    PaymentResponse createPaymentOrder(PaymentRequest request) throws Exception;

    PaymentResponse verifyPayment(PaymentVerificationRequest request) throws Exception;

    PaymentResponse markPaymentFailed(PaymentFailureRequest request);

    PaymentResponse cancelPayment(PaymentFailureRequest request);

    PaymentResponse getPaymentByOrderId(Long orderId);

    PaymentResponse getPaymentById(Long paymentId);
}
