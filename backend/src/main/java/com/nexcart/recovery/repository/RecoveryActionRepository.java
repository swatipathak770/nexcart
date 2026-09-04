package com.nexcart.recovery.repository;
import com.nexcart.recovery.entity.RecoveryAction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface RecoveryActionRepository extends JpaRepository<RecoveryAction,Long> { List<RecoveryAction> findByRecoveryCaseIdOrderByCreatedAtAsc(Long id); Optional<RecoveryAction> findByRazorpayPaymentLinkId(String razorpayPaymentLinkId); }
