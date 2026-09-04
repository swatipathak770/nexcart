package com.nexcart.recovery.entity;

import com.nexcart.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name="recovery_audit_logs", indexes=@Index(name="idx_recovery_audit_case", columnList="recovery_case_id")) @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RecoveryAuditLog extends BaseEntity {
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="recovery_case_id", nullable=false) private RecoveryCase recoveryCase;
 @Column(nullable=false) private String eventType;
 @Column(length=2000) private String metadata;
}
