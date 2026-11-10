package com.nexcart.service;

import com.nexcart.dto.response.PaymentResponse;
import com.nexcart.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AdminPaymentService {

    Page<PaymentResponse> getAllPayments(Pageable pageable);

    PaymentResponse getPaymentById(Long paymentId);

    PaymentResponse getPaymentByOrderId(Long orderId);

    Page<PaymentResponse> getPaymentsByStatus(PaymentStatus status, Pageable pageable);

}
