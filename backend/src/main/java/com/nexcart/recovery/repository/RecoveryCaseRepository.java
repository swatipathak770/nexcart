package com.nexcart.recovery.repository;
import com.nexcart.recovery.entity.RecoveryCase;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface RecoveryCaseRepository extends JpaRepository<RecoveryCase,Long> {
 @Override @EntityGraph(attributePaths = "order") List<RecoveryCase> findAll();
 @Override @EntityGraph(attributePaths = "order") Optional<RecoveryCase> findById(Long id);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select c from RecoveryCase c left join fetch c.order where c.id = :id") Optional<RecoveryCase> findByIdForUpdate(@Param("id") Long id);
 @EntityGraph(attributePaths = "order") Optional<RecoveryCase> findFirstByOrderIdOrderByCreatedAtDesc(Long orderId);
 @EntityGraph(attributePaths = "order") List<RecoveryCase> findBySimulated(boolean simulated);
}
