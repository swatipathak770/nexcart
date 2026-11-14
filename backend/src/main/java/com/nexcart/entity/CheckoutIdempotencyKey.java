package com.nexcart.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "checkout_idempotency_keys",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_checkout_idempotency_user_key", columnNames = {"user_id", "idempotency_key"}),
                @UniqueConstraint(name = "uk_checkout_idempotency_order", columnNames = "order_id")
        })
@Getter
@Setter
@NoArgsConstructor
public class CheckoutIdempotencyKey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_checkout_idempotency_user"))
    private User user;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id",
            foreignKey = @ForeignKey(name = "fk_checkout_idempotency_order"))
    private Order order;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
