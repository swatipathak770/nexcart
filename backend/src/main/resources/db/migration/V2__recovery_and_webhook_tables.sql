CREATE TABLE IF NOT EXISTS `recovery_cases` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `action_attempts` int DEFAULT NULL,
  `amount` decimal(12,2) NOT NULL,
  `confidence` varchar(255) DEFAULT NULL,
  `currency` varchar(3) NOT NULL,
  `decision_reason` varchar(1000) DEFAULT NULL,
  `decision_source` varchar(100) DEFAULT NULL,
  `executed_action` enum('CREATE_PAYMENT_LINK','NO_ACTION','RETRY_PAYMENT','SEND_RECOVERY_MESSAGE') DEFAULT NULL,
  `expected_recovery_amount` decimal(12,2) DEFAULT NULL,
  `failure_reason` varchar(500) DEFAULT NULL,
  `guardrail_result` varchar(500) DEFAULT NULL,
  `razorpay_payment_id` varchar(255) DEFAULT NULL,
  `recommended_action` enum('CREATE_PAYMENT_LINK','NO_ACTION','RETRY_PAYMENT','SEND_RECOVERY_MESSAGE') DEFAULT NULL,
  `recovered_amount` decimal(12,2) DEFAULT NULL,
  `recovery_probability` decimal(5,4) DEFAULT NULL,
  `recovery_score` int DEFAULT NULL,
  `resolved_at` datetime(6) DEFAULT NULL,
  `risk_level` varchar(255) DEFAULT NULL,
  `simulated` bit(1) NOT NULL,
  `status` enum('ACTION_PENDING','ACTION_RECOMMENDED','ANALYZING','CUSTOMER_CANCELLED','DETECTED','EXHAUSTED','FAILED','NO_ACTION','RECOVERED') NOT NULL,
  `type` enum('CHECKOUT_ABANDONED','PAYMENT_FAILED') NOT NULL,
  `order_id` bigint DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKq5b3v60eolmb5rwh29le0ghsy` (`razorpay_payment_id`),
  KEY `idx_recovery_order` (`order_id`),
  KEY `idx_recovery_status` (`status`),
  KEY `idx_recovery_created` (`created_at`),
  KEY `FKf2hs9mbxo9wokp950ku41q1ca` (`user_id`),
  CONSTRAINT `FK33plhl7b59tfq6ohbbrw40jof` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`),
  CONSTRAINT `FKf2hs9mbxo9wokp950ku41q1ca` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `recovery_actions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `action` enum('CREATE_PAYMENT_LINK','NO_ACTION','RETRY_PAYMENT','SEND_RECOVERY_MESSAGE') NOT NULL,
  `failure_detail` varchar(500) DEFAULT NULL,
  `message` varchar(1000) DEFAULT NULL,
  `payment_link` varchar(1000) DEFAULT NULL,
  `razorpay_payment_link_id` varchar(255) DEFAULT NULL,
  `successful` bit(1) NOT NULL,
  `recovery_case_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKowvjr4jqhvkwsoisuaoy95u04` (`razorpay_payment_link_id`),
  KEY `FK6ihul8kdflg5tcma1s29t312m` (`recovery_case_id`),
  CONSTRAINT `FK6ihul8kdflg5tcma1s29t312m` FOREIGN KEY (`recovery_case_id`) REFERENCES `recovery_cases` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `recovery_audit_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `event_type` varchar(255) NOT NULL,
  `metadata` varchar(2000) DEFAULT NULL,
  `recovery_case_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_recovery_audit_case` (`recovery_case_id`),
  CONSTRAINT `FKm17bun8dst93mjidrnajs67xi` FOREIGN KEY (`recovery_case_id`) REFERENCES `recovery_cases` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `processed_webhook_events` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `event_id` varchar(128) NOT NULL,
  `event_type` varchar(100) NOT NULL,
  `processed_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKa0j09e3qy71s5m3b0p3tu7efa` (`event_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
