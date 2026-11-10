package com.nexcart.recovery.repository;
import com.nexcart.recovery.entity.RecoveryAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
public interface RecoveryAuditRepository extends JpaRepository<RecoveryAuditLog,Long> {
 List<RecoveryAuditLog> findByRecoveryCaseIdOrderByCreatedAtAsc(Long id);
 Page<RecoveryAuditLog> findByRecoveryCaseIdOrderByCreatedAtDesc(Long id, Pageable pageable);
}
