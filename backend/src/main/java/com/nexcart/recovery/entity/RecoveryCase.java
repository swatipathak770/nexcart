package com.nexcart.recovery.entity;

import com.nexcart.entity.BaseEntity;
import com.nexcart.entity.Order;
import com.nexcart.entity.User;
import com.nexcart.recovery.enums.*;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name="recovery_cases", indexes={@Index(name="idx_recovery_order", columnList="order_id"), @Index(name="idx_recovery_status", columnList="status"), @Index(name="idx_recovery_created", columnList="createdAt")})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RecoveryCase extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="order_id") private Order order;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id") private User user;
    @Column(unique=true) private String razorpayPaymentId;
    @Column(nullable=false, precision=12, scale=2) private BigDecimal amount;
    @Builder.Default @Column(nullable=false, length=3) private String currency="INR";
    @Enumerated(EnumType.STRING) @Column(nullable=false) private RecoveryType type;
    @Column(length=500) private String failureReason;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private RecoveryStatus status;
    private Integer recoveryScore;
    @Column(precision=5, scale=4) private BigDecimal recoveryProbability;
    @Column(precision=12, scale=2) private BigDecimal expectedRecoveryAmount;
    @Enumerated(EnumType.STRING) private RecoveryActionType recommendedAction;
    @Enumerated(EnumType.STRING) private RecoveryActionType executedAction;
    @Builder.Default private Integer actionAttempts=0;
    @Builder.Default @Column(precision=12, scale=2) private BigDecimal recoveredAmount=BigDecimal.ZERO;
    @Builder.Default private boolean simulated=false;
    @Column(length=1000) private String decisionReason;
    private String confidence;
    private String riskLevel;
    @Column(length=100) private String decisionSource;
    @Column(length=500) private String guardrailResult;
    private LocalDateTime resolvedAt;
}
