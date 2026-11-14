package com.nexcart.repository;
import com.nexcart.entity.PaymentAttempt;
import com.nexcart.entity.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt,Long> {
 List<PaymentAttempt> findByPaymentIdOrderByAttemptNumberAsc(Long paymentId);
 List<PaymentAttempt> findByPaymentIdAndStatusOrderByAttemptNumberDesc(Long paymentId, PaymentStatus status);
 Optional<PaymentAttempt> findFirstByPaymentIdOrderByAttemptNumberDesc(Long paymentId);
 Optional<PaymentAttempt> findByRazorpayOrderId(String orderId);
 Optional<PaymentAttempt> findByRazorpayPaymentId(String paymentId);
 Optional<PaymentAttempt> findByIdempotencyKey(String key);
 boolean existsByPaymentIdAndStatus(Long paymentId, PaymentStatus status);
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select a from PaymentAttempt a where a.payment.id=:paymentId and a.status=:status")
 List<PaymentAttempt> findByPaymentIdAndStatusForUpdate(@Param("paymentId") Long paymentId, @Param("status") PaymentStatus status);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select a from PaymentAttempt a where a.razorpayOrderId=:id") Optional<PaymentAttempt> findByRazorpayOrderIdForUpdate(@Param("id") String id);
}
