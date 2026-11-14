package com.nexcart.repository;

import com.nexcart.entity.CheckoutIdempotencyKey;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Collection;

public interface CheckoutIdempotencyKeyRepository extends JpaRepository<CheckoutIdempotencyKey, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select k from CheckoutIdempotencyKey k where k.user.id = :userId and k.idempotencyKey = :key")
    Optional<CheckoutIdempotencyKey> findByUserAndKeyForUpdate(@Param("userId") Long userId,
                                                               @Param("key") String key);

    @Modifying
    @Query("delete from CheckoutIdempotencyKey k where k.user.id in :userIds")
    void deleteByUserIds(@Param("userIds") Collection<Long> userIds);
}
