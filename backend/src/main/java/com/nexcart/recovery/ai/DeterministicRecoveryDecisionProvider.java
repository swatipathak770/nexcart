package com.nexcart.recovery.ai;

import com.nexcart.recovery.dto.RecoveryContext;
import com.nexcart.recovery.dto.RecoveryDecision;
import com.nexcart.recovery.enums.RecoveryActionType;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class DeterministicRecoveryDecisionProvider implements RecoveryDecisionProvider {
    @Override public RecoveryDecision decide(RecoveryContext context) {
        BigDecimal probability = context.amount().compareTo(new BigDecimal("3000")) >= 0 ? new BigDecimal("0.87") : new BigDecimal("0.58");
        if (context.recoveryAttemptCount() >= 2) probability = new BigDecimal("0.25");
        RecoveryActionType action = context.recoveryAttemptCount() >= 2 ? RecoveryActionType.SEND_RECOVERY_MESSAGE : context.amount().compareTo(new BigDecimal("1000")) >= 0 ? RecoveryActionType.CREATE_PAYMENT_LINK : RecoveryActionType.RETRY_PAYMENT;
        String reason = action == RecoveryActionType.CREATE_PAYMENT_LINK ? "Higher-value payment at risk. A secure payment link gives the customer a supported way to complete checkout." : action == RecoveryActionType.RETRY_PAYMENT ? "A first retry is permitted for this lower-risk payment failure." : "Previous attempts reached the safe retry limit; send one bounded recovery reminder.";
        return new RecoveryDecision(action, probability, probability.compareTo(new BigDecimal("0.80")) >= 0 ? new BigDecimal("0.85") : new BigDecimal("0.70"), reason, context.recoveryAttemptCount() >= 2 ? "MEDIUM" : "LOW");
    }
}
