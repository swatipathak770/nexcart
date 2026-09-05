package com.nexcart.recovery.ai;

import com.nexcart.recovery.dto.RecoveryContext;
import com.nexcart.recovery.dto.RecoveryDecision;

public interface RecoveryDecisionProvider {
    RecoveryDecision decide(RecoveryContext context) throws Exception;
}
