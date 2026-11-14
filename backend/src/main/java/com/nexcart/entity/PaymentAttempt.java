package com.nexcart.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name="payment_attempts", uniqueConstraints={
 @UniqueConstraint(name="uk_payment_attempt_number", columnNames={"payment_id","attempt_number"}),
 @UniqueConstraint(name="uk_payment_attempt_razorpay_order", columnNames="razorpay_order_id"),
 @UniqueConstraint(name="uk_payment_attempt_razorpay_payment", columnNames="razorpay_payment_id"),
 @UniqueConstraint(name="uk_payment_attempt_idempotency", columnNames="idempotency_key")
}, indexes={@Index(name="idx_payment_attempt_payment_status", columnList="payment_id,status"),
 @Index(name="idx_payment_attempt_recovery_case", columnList="recovery_case_id")})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentAttempt {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="payment_id",nullable=false) private Payment payment;
 @Column(name="attempt_number",nullable=false) private Integer attemptNumber;
 @Column(nullable=false,precision=12,scale=2) private BigDecimal amount;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private PaymentStatus status;
 @Column(name="razorpay_order_id") private String razorpayOrderId;
 @Column(name="razorpay_payment_id") private String razorpayPaymentId;
 @Column(name="razorpay_signature",length=500) private String razorpaySignature;
 @Column(name="failure_reason",length=500) private String failureReason;
 @Column(name="recovery_case_id") private Long recoveryCaseId;
 @Column(name="idempotency_key",length=100) private String idempotencyKey;
 @Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;
 @Column(name="updated_at",nullable=false) private LocalDateTime updatedAt;
 @PrePersist void createTimes(){createdAt=LocalDateTime.now();updatedAt=createdAt;}
 @PreUpdate void updateTime(){updatedAt=LocalDateTime.now();}
}
