package com.nexcart.recovery.repository;
import com.nexcart.recovery.entity.RecoveryCase;
import com.nexcart.recovery.enums.RecoveryStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.*;
public interface RecoveryCaseRepository extends JpaRepository<RecoveryCase,Long> {
 @Query("select c.order.id from RecoveryCase c where c.id = :id")
 Optional<Long> findOrderIdById(@Param("id") Long id);
 @Query("""
     select coalesce(sum(case when c.status <> :recovered and c.status <> :cancelled then c.amount else 0 end), 0) as revenueAtRisk,
            coalesce(sum(case when c.status = :recovered then coalesce(c.recoveredAmount, 0) else 0 end), 0) as revenueRecovered,
            coalesce(sum(case when c.status not in :terminalStatuses then coalesce(c.expectedRecoveryAmount, 0) else 0 end), 0) as expectedRecoverable,
            sum(case when c.status = :recovered and c.status <> :cancelled then 1 else 0 end) as recoveredCases,
            count(c) as totalCases
       from RecoveryCase c
      where c.simulated = :simulated
     """)
 RecoveryMetricsProjection aggregateMetrics(@Param("simulated") boolean simulated,
                                            @Param("recovered") RecoveryStatus recovered,
                                            @Param("cancelled") RecoveryStatus cancelled,
                                            @Param("terminalStatuses") Set<RecoveryStatus> terminalStatuses);

 @Query("""
     select coalesce(sum(case when c.status <> :recovered and c.status <> :cancelled then c.amount else 0 end), 0) as revenueAtRisk,
            coalesce(sum(case when c.status = :recovered then coalesce(c.recoveredAmount, 0) else 0 end), 0) as revenueRecovered,
            coalesce(sum(case when c.status not in :terminalStatuses then coalesce(c.expectedRecoveryAmount, 0) else 0 end), 0) as expectedRecoverable,
            sum(case when c.status = :recovered and c.status <> :cancelled then 1 else 0 end) as recoveredCases,
            count(c) as totalCases
       from RecoveryCase c
     """)
 RecoveryMetricsProjection aggregateAllMetrics(@Param("recovered") RecoveryStatus recovered,
                                                @Param("cancelled") RecoveryStatus cancelled,
                                                @Param("terminalStatuses") Set<RecoveryStatus> terminalStatuses);

 @Query("select c.recommendedAction, count(c) from RecoveryCase c where c.simulated = :simulated and c.recommendedAction is not null group by c.recommendedAction")
 List<Object[]> countActionsByRecommendation(@Param("simulated") boolean simulated);

 @Query("select c.recommendedAction, count(c) from RecoveryCase c where c.recommendedAction is not null group by c.recommendedAction")
 List<Object[]> countAllActionsByRecommendation();
 @Override @EntityGraph(attributePaths = "order") Page<RecoveryCase> findAll(Pageable pageable);
 @Override @EntityGraph(attributePaths = "order") Optional<RecoveryCase> findById(Long id);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select c from RecoveryCase c left join fetch c.order where c.id = :id") Optional<RecoveryCase> findByIdForUpdate(@Param("id") Long id);
 @EntityGraph(attributePaths = "order") Optional<RecoveryCase> findFirstByOrderIdOrderByCreatedAtDesc(Long orderId);
 @EntityGraph(attributePaths = "order") Page<RecoveryCase> findBySimulated(boolean simulated, Pageable pageable);
 @EntityGraph(attributePaths = "order") List<RecoveryCase> findBySimulated(boolean simulated);
}
