package com.nexcart.recovery.entity;

import com.nexcart.entity.BaseEntity;
import com.nexcart.recovery.enums.RecoveryActionType;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name="recovery_actions") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RecoveryAction extends BaseEntity {
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="recovery_case_id", nullable=false) private RecoveryCase recoveryCase;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private RecoveryActionType action;
 @Column(unique=true) private String razorpayPaymentLinkId;
 @Column(length=1000) private String paymentLink;
 @Column(length=1000) private String message;
 private boolean successful;
 @Column(length=500) private String failureDetail;
}
