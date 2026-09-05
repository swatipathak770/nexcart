package com.nexcart.recovery.dto;

import com.nexcart.recovery.enums.RecoveryActionType;
import java.math.BigDecimal;

/** A validated recommendation only; the backend owns all money calculations and execution. */
public record RecoveryDecision(RecoveryActionType recommendedAction, BigDecimal recoveryProbability,
                               BigDecimal confidence, String reason, String riskLevel) { }
