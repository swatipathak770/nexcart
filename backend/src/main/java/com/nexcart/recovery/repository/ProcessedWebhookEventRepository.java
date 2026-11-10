package com.nexcart.recovery.repository;

import com.nexcart.recovery.entity.ProcessedWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedWebhookEventRepository extends JpaRepository<ProcessedWebhookEvent, Long> {
    boolean existsByEventId(String eventId);
}
