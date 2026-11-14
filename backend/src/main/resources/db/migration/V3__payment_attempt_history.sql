CREATE TABLE payment_attempts (
 id BIGINT NOT NULL AUTO_INCREMENT,
 payment_id BIGINT NOT NULL,
 attempt_number INT NOT NULL,
 amount DECIMAL(12,2) NOT NULL,
 status VARCHAR(20) NOT NULL,
 razorpay_order_id VARCHAR(255) NULL,
 razorpay_payment_id VARCHAR(255) NULL,
 razorpay_signature VARCHAR(500) NULL,
 failure_reason VARCHAR(500) NULL,
 recovery_case_id BIGINT NULL,
 idempotency_key VARCHAR(100) NULL,
 created_at DATETIME(6) NOT NULL,
 updated_at DATETIME(6) NOT NULL,
 PRIMARY KEY (id),
 CONSTRAINT uk_payment_attempt_number UNIQUE (payment_id,attempt_number),
 CONSTRAINT uk_payment_attempt_razorpay_order UNIQUE (razorpay_order_id),
 CONSTRAINT uk_payment_attempt_razorpay_payment UNIQUE (razorpay_payment_id),
 CONSTRAINT uk_payment_attempt_idempotency UNIQUE (idempotency_key),
 KEY idx_payment_attempt_payment_status (payment_id,status),
 KEY idx_payment_attempt_recovery_case (recovery_case_id),
 CONSTRAINT fk_payment_attempt_payment FOREIGN KEY (payment_id) REFERENCES payments(id),
 CONSTRAINT fk_payment_attempt_recovery_case FOREIGN KEY (recovery_case_id) REFERENCES recovery_cases(id)
) ENGINE=InnoDB;

INSERT INTO payment_attempts
 (payment_id,attempt_number,amount,status,razorpay_order_id,razorpay_payment_id,razorpay_signature,failure_reason,created_at,updated_at)
SELECT id,1,amount,payment_status,razorpay_order_id,razorpay_payment_id,razorpay_signature,failure_reason,
 COALESCE(created_at,CURRENT_TIMESTAMP(6)),COALESCE(updated_at,created_at,CURRENT_TIMESTAMP(6))
FROM payments;
