package com.nexcart.recovery.ai;
import com.nexcart.recovery.dto.RecoveryDecision;
import com.nexcart.recovery.entity.RecoveryCase;
public interface AIRecoveryService { RecoveryDecision decide(RecoveryCase recoveryCase); }
