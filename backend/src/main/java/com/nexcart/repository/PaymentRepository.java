package com.nexcart.repository;

import com.nexcart.entity.Order;
import com.nexcart.entity.Payment;
import com.nexcart.entity.PaymentStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @EntityGraph(attributePaths = {"order"})
    Page<Payment> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"order"})
    Optional<Payment> findById(Long id);

    @EntityGraph(attributePaths = {"order"})
    Optional<Payment> findByOrder(Order order);

    @EntityGraph(attributePaths = {"order"})
    Optional<Payment> findByOrderId(Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p join fetch p.order where p.order.id = :orderId")
    Optional<Payment> findByOrderIdForUpdate(@Param("orderId") Long orderId);

    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p join fetch p.order where p.razorpayOrderId = :razorpayOrderId")
    Optional<Payment> findByRazorpayOrderIdForUpdate(@Param("razorpayOrderId") String razorpayOrderId);

    Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);

    @EntityGraph(attributePaths = {"order"})
    Page<Payment> findByPaymentStatus(PaymentStatus paymentStatus, Pageable pageable);

    boolean existsByOrderId(Long orderId);
}
