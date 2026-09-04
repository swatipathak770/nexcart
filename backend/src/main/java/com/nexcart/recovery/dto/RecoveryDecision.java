package com.nexcart.recovery.dto;
import com.nexcart.recovery.enums.RecoveryActionType;
import java.math.BigDecimal;
public record RecoveryDecision(RecoveryActionType recommendedAction, BigDecimal recoveryProbability, BigDecimal expectedRecoveryAmount, String confidence, String reason, String riskLevel) {}
