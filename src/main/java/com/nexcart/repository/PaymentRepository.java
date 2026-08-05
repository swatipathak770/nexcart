package com.nexcart.repository;

import com.nexcart.entity.Order;
import com.nexcart.entity.Payment;
import com.nexcart.entity.PaymentStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Override
    @EntityGraph(attributePaths = {"order"})
    List<Payment> findAll();

    @Override
    @EntityGraph(attributePaths = {"order"})
    Optional<Payment> findById(Long id);

    @EntityGraph(attributePaths = {"order"})
    Optional<Payment> findByOrder(Order order);

    @EntityGraph(attributePaths = {"order"})
    Optional<Payment> findByOrderId(Long orderId);

    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);

    @EntityGraph(attributePaths = {"order"})
    List<Payment> findByPaymentStatus(PaymentStatus paymentStatus);

    boolean existsByOrderId(Long orderId);
}
