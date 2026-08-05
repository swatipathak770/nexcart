package com.nexcart.service.impl;

import com.nexcart.dto.response.PaymentResponse;
import com.nexcart.entity.Payment;
import com.nexcart.entity.PaymentStatus;
import com.nexcart.exception.ResourceNotFoundException;
import com.nexcart.mapper.PaymentMapper;
import com.nexcart.repository.PaymentRepository;
import com.nexcart.service.AdminPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminPaymentServiceImpl implements AdminPaymentService {

    private final PaymentRepository paymentRepository;

    @Override
    public List<PaymentResponse> getAllPayments() {

        return paymentRepository.findAll()
                .stream()
                .map(PaymentMapper::toResponse)
                .toList();
    }

    @Override
    public PaymentResponse getPaymentById(Long paymentId) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Payment not found."));

        return PaymentMapper.toResponse(payment);
    }

    @Override
    public PaymentResponse getPaymentByOrderId(Long orderId) {

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Payment not found."));

        return PaymentMapper.toResponse(payment);
    }

    @Override
    public List<PaymentResponse> getPaymentsByStatus(PaymentStatus status) {

        return paymentRepository.findByPaymentStatus(status)
                .stream()
                .map(PaymentMapper::toResponse)
                .toList();
    }
}