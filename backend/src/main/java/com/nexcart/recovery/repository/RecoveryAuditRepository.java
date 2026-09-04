package com.nexcart.recovery.repository;
import com.nexcart.recovery.entity.RecoveryAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface RecoveryAuditRepository extends JpaRepository<RecoveryAuditLog,Long> { List<RecoveryAuditLog> findByRecoveryCaseIdOrderByCreatedAtAsc(Long id); }
