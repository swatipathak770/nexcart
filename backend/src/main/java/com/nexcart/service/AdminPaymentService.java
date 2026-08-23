package com.nexcart.service;

import com.nexcart.dto.response.PaymentResponse;
import com.nexcart.entity.PaymentStatus;

import java.util.List;

public interface AdminPaymentService {

    List<PaymentResponse> getAllPayments();

    PaymentResponse getPaymentById(Long paymentId);

    PaymentResponse getPaymentByOrderId(Long orderId);

    List<PaymentResponse> getPaymentsByStatus(PaymentStatus status);

}
