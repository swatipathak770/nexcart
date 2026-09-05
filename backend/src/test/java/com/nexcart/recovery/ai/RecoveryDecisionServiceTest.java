package com.nexcart.recovery.ai;

import com.nexcart.recovery.dto.RecoveryContext;
import com.nexcart.recovery.dto.RecoveryDecision;
import com.nexcart.recovery.dto.RecoveryDecisionResult;
import com.nexcart.recovery.enums.RecoveryActionType;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class RecoveryDecisionServiceTest {
    private final AiRecoveryDecisionProvider ai = mock(AiRecoveryDecisionProvider.class);
    private final DeterministicRecoveryDecisionProvider fallback = mock(DeterministicRecoveryDecisionProvider.class);
    private final RecoveryDecisionService service = new RecoveryDecisionService(ai, fallback);
    private final RecoveryContext context = new RecoveryContext(1L, 2L, new BigDecimal("1799.00"), "INR", "FAILED", "Timeout", 1, 0, List.of(), "DETECTED", "PENDING", false, 1, false);

    @Test
    void validAiDecisionIsUsed() throws Exception {
        RecoveryDecision decision = decision(RecoveryActionType.CREATE_PAYMENT_LINK, "0.58");
        when(ai.isConfigured()).thenReturn(true); when(ai.decide(context)).thenReturn(decision);
        RecoveryDecisionResult result = service.decide(context);
        assertEquals("AI", result.source()); assertEquals(decision, result.decision()); verifyNoInteractions(fallback);
    }

    @Test
    void invalidAiDecisionFallsBack() throws Exception {
        when(ai.isConfigured()).thenReturn(true); when(ai.decide(context)).thenReturn(decision(RecoveryActionType.CREATE_PAYMENT_LINK, "1.10"));
        when(fallback.decide(context)).thenReturn(decision(RecoveryActionType.RETRY_PAYMENT, "0.58"));
        RecoveryDecisionResult result = service.decide(context);
        assertEquals("DETERMINISTIC_FALLBACK", result.source()); assertEquals(RecoveryActionType.RETRY_PAYMENT, result.decision().recommendedAction());
    }

    @Test
    void unsupportedAiActionFallsBack() throws Exception {
        when(ai.isConfigured()).thenReturn(true); when(ai.decide(context)).thenReturn(decision(RecoveryActionType.SEND_RECOVERY_MESSAGE, "0.58"));
        when(fallback.decide(context)).thenReturn(decision(RecoveryActionType.RETRY_PAYMENT, "0.58"));

        assertEquals("DETERMINISTIC_FALLBACK", service.decide(context).source());
    }

    @Test
    void unavailableAiFallsBack() {
        when(ai.isConfigured()).thenReturn(false); when(fallback.decide(context)).thenReturn(decision(RecoveryActionType.RETRY_PAYMENT, "0.58"));
        assertEquals("DETERMINISTIC_FALLBACK", service.decide(context).source());
    }

    @Test
    void parsesGeminiStructuredDecision() throws Exception {
        AiRecoveryDecisionProvider provider = new AiRecoveryDecisionProvider(
                new com.fasterxml.jackson.databind.ObjectMapper(), RestClient.builder(), "test-key", "gemini-2.5-flash");
        String response = """
                {"candidates":[{"content":{"parts":[{"text":"{\\\"action\\\":\\\"CREATE_PAYMENT_LINK\\\",\\\"recoveryProbability\\\":0.58,\\\"confidence\\\":0.7,\\\"riskLevel\\\":\\\"LOW\\\",\\\"reason\\\":\\\"Use a secure payment link.\\\"}"}]}}]}
                """;

        RecoveryDecision result = provider.parse(response);

        assertEquals(RecoveryActionType.CREATE_PAYMENT_LINK, result.recommendedAction());
        assertEquals(new BigDecimal("0.58"), result.recoveryProbability());
        assertEquals(new BigDecimal("0.7"), result.confidence());
        assertEquals("LOW", result.riskLevel());
    }

    private RecoveryDecision decision(RecoveryActionType action, String probability) {
        return new RecoveryDecision(action, new BigDecimal(probability), new BigDecimal("0.80"), "Safe bounded recommendation.", "LOW");
    }
}
