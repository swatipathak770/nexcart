package com.nexcart.recovery.ai;

import com.nexcart.recovery.dto.*;
import com.nexcart.recovery.enums.RecoveryActionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.Set;

@Service @RequiredArgsConstructor
public class RecoveryDecisionService {
    private static final Set<String> RISKS = Set.of("LOW", "MEDIUM", "HIGH");
    private static final Set<RecoveryActionType> ACTIONS = Set.of(
            RecoveryActionType.RETRY_PAYMENT, RecoveryActionType.CREATE_PAYMENT_LINK, RecoveryActionType.NO_ACTION);
    private final AiRecoveryDecisionProvider aiProvider;
    private final DeterministicRecoveryDecisionProvider fallbackProvider;
    public RecoveryDecisionResult decide(RecoveryContext context) {
        try {
            if (!aiProvider.isConfigured()) throw new IllegalStateException("GEMINI_API_KEY is not configured");
            return new RecoveryDecisionResult(validate(aiProvider.decide(context)), "AI", null);
        } catch (Exception ex) {
            return new RecoveryDecisionResult(validate(fallbackProvider.decide(context)), "DETERMINISTIC_FALLBACK", safeMessage(ex));
        }
    }
    public RecoveryDecision validate(RecoveryDecision decision) {
        if (decision == null || decision.recommendedAction() == null || !ACTIONS.contains(decision.recommendedAction()) || decision.recommendedAction() == RecoveryActionType.NO_ACTION && (decision.reason() == null || decision.reason().isBlank())) throw new IllegalArgumentException("Missing or unsupported action, or missing reason");
        if (decision.recoveryProbability() == null || decision.recoveryProbability().compareTo(BigDecimal.ZERO) < 0 || decision.recoveryProbability().compareTo(BigDecimal.ONE) > 0) throw new IllegalArgumentException("Invalid recovery probability");
        if (decision.confidence() == null || decision.confidence().compareTo(BigDecimal.ZERO) < 0 || decision.confidence().compareTo(BigDecimal.ONE) > 0) throw new IllegalArgumentException("Invalid confidence");
        if (!RISKS.contains(decision.riskLevel()) || decision.reason() == null || decision.reason().isBlank() || decision.reason().length() > 500) throw new IllegalArgumentException("Invalid risk level or reason");
        return decision;
    }
    private String safeMessage(Exception ex) {
        return "Gemini decision unavailable (" + ex.getClass().getSimpleName() + ")";
    }
}
