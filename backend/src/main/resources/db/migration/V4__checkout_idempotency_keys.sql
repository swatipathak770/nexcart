CREATE TABLE checkout_idempotency_keys (
 id BIGINT NOT NULL AUTO_INCREMENT,
 user_id BIGINT NOT NULL,
 idempotency_key VARCHAR(100) NOT NULL,
 order_id BIGINT NULL,
 created_at DATETIME(6) NOT NULL,
 PRIMARY KEY (id),
 CONSTRAINT uk_checkout_idempotency_user_key UNIQUE (user_id, idempotency_key),
 CONSTRAINT uk_checkout_idempotency_order UNIQUE (order_id),
 CONSTRAINT fk_checkout_idempotency_user FOREIGN KEY (user_id) REFERENCES users(id),
 CONSTRAINT fk_checkout_idempotency_order FOREIGN KEY (order_id) REFERENCES orders(id)
) ENGINE=InnoDB;
