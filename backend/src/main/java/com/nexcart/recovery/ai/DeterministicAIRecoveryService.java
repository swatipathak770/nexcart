package com.nexcart.recovery.ai;
import com.nexcart.recovery.dto.RecoveryDecision;
import com.nexcart.recovery.entity.RecoveryCase;
import com.nexcart.recovery.enums.RecoveryActionType;
import org.springframework.stereotype.Service;
import java.math.*;

@Service
public class DeterministicAIRecoveryService implements AIRecoveryService {
 public RecoveryDecision decide(RecoveryCase c) {
  BigDecimal probability = c.getAmount().compareTo(new BigDecimal("3000")) >= 0 ? new BigDecimal("0.87") : new BigDecimal("0.58");
  if (c.getActionAttempts() >= 2) probability = new BigDecimal("0.25");
  RecoveryActionType action = c.getActionAttempts() >= 2 ? RecoveryActionType.SEND_RECOVERY_MESSAGE : c.getAmount().compareTo(new BigDecimal("1000")) >= 0 ? RecoveryActionType.CREATE_PAYMENT_LINK : RecoveryActionType.RETRY_PAYMENT;
  String reason = action == RecoveryActionType.CREATE_PAYMENT_LINK ? "Higher-value payment at risk. A secure payment link gives the customer a clear, supported way to complete checkout." : action == RecoveryActionType.RETRY_PAYMENT ? "A first retry is permitted for this lower-risk payment failure." : "Previous attempts reached the safe retry limit; send one bounded recovery reminder.";
  return new RecoveryDecision(action, probability, c.getAmount().multiply(probability).setScale(2, RoundingMode.HALF_UP), probability.compareTo(new BigDecimal("0.80")) >= 0 ? "HIGH" : "MEDIUM", reason, c.getActionAttempts() >= 2 ? "MEDIUM" : "LOW");
 }
}
