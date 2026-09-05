package com.nexcart.recovery.service;

import com.nexcart.recovery.ai.RecoveryDecisionService;
import com.nexcart.recovery.entity.RecoveryAction;
import com.nexcart.recovery.entity.RecoveryCase;
import com.nexcart.recovery.enums.RecoveryActionType;
import com.nexcart.recovery.enums.RecoveryStatus;
import com.nexcart.recovery.dto.RecoveryDecision;
import com.nexcart.recovery.dto.RecoveryDecisionResult;
import com.nexcart.recovery.repository.RecoveryActionRepository;
import com.nexcart.recovery.repository.RecoveryAuditRepository;
import com.nexcart.recovery.repository.RecoveryCaseRepository;
import com.nexcart.repository.OrderRepository;
import com.nexcart.repository.PaymentRepository;
import com.razorpay.RazorpayClient;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class RecoveryServiceTest {
    private final RecoveryCaseRepository cases = mock(RecoveryCaseRepository.class);
    private final RecoveryActionRepository actions = mock(RecoveryActionRepository.class);
    private final RecoveryAuditRepository audits = mock(RecoveryAuditRepository.class);
    private final PaymentRepository payments = mock(PaymentRepository.class);
    private final OrderRepository orders = mock(OrderRepository.class);
    private final RecoveryDecisionService decisions = mock(RecoveryDecisionService.class);
    private final RazorpayClient razorpay = mock(RazorpayClient.class);
    private final RecoveryService service = new RecoveryService(cases, actions, audits, payments, orders, decisions, razorpay);
    {
        ReflectionTestUtils.setField(service, "maxAttempts", 3);
    }

    @ParameterizedTest
    @EnumSource(value = RecoveryStatus.class, names = {"RECOVERED", "CUSTOMER_CANCELLED", "EXHAUSTED", "FAILED", "NO_ACTION"})
    void terminalCasesCannotExecuteAnotherAction(RecoveryStatus status) {
        RecoveryCase recoveryCase = RecoveryCase.builder().status(status).build();
        when(cases.findByIdForUpdate(1L)).thenReturn(Optional.of(recoveryCase));

        assertThrows(IllegalStateException.class, () -> service.execute(1L));
        verifyNoInteractions(actions, razorpay);
    }

    @org.junit.jupiter.api.Test
    void completedEquivalentPaymentLinkIsNotCreatedAgain() {
        RecoveryCase recoveryCase = RecoveryCase.builder()
                .status(RecoveryStatus.ACTION_RECOMMENDED)
                .recommendedAction(RecoveryActionType.CREATE_PAYMENT_LINK)
                .build();
        RecoveryAction completedLink = RecoveryAction.builder()
                .recoveryCase(recoveryCase)
                .action(RecoveryActionType.CREATE_PAYMENT_LINK)
                .paymentLink("https://rzp.io/recovery")
                .successful(true)
                .build();
        when(cases.findByIdForUpdate(1L)).thenReturn(Optional.of(recoveryCase));
        when(actions.findByRecoveryCaseIdOrderByCreatedAtAsc(null)).thenReturn(List.of(completedLink));
        when(cases.save(recoveryCase)).thenReturn(recoveryCase);

        service.execute(1L);

        verifyNoInteractions(razorpay);
        verify(actions, never()).save(any());
    }

    @org.junit.jupiter.api.Test
    void simulatedPaymentLinkRecoversWithoutCallingRazorpay() {
        RecoveryCase recoveryCase = RecoveryCase.builder()
                .amount(new BigDecimal("4999.00"))
                .status(RecoveryStatus.ACTION_RECOMMENDED)
                .recommendedAction(RecoveryActionType.CREATE_PAYMENT_LINK)
                .simulated(true)
                .build();
        when(cases.findByIdForUpdate(1L)).thenReturn(Optional.of(recoveryCase));
        when(actions.findByRecoveryCaseIdOrderByCreatedAtAsc(null)).thenReturn(List.of());
        when(cases.save(recoveryCase)).thenReturn(recoveryCase);

        service.execute(1L);

        verifyNoInteractions(razorpay);
        verify(actions).save(argThat(action -> action.isSuccessful()
                && action.getPaymentLink().startsWith("simulated://payment-links/REC-")));
        org.junit.jupiter.api.Assertions.assertEquals(RecoveryStatus.RECOVERED, recoveryCase.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("4999.00"), recoveryCase.getRecoveredAmount());
    }

    @org.junit.jupiter.api.Test
    void validatedAiRecommendationPassesGuardrailsAndExecutesInSimulation() {
        RecoveryCase recoveryCase = RecoveryCase.builder().amount(new BigDecimal("1799.00"))
                .status(RecoveryStatus.DETECTED).simulated(true).build();
        RecoveryDecision decision = new RecoveryDecision(RecoveryActionType.CREATE_PAYMENT_LINK,
                new BigDecimal("0.58"), new BigDecimal("0.80"), "A secure link is the safest option.", "LOW");
        when(cases.findByIdForUpdate(1L)).thenReturn(Optional.of(recoveryCase));
        when(actions.findByRecoveryCaseIdOrderByCreatedAtAsc(null)).thenReturn(List.of());
        when(decisions.decide(any())).thenReturn(new RecoveryDecisionResult(decision, "AI", null));
        when(cases.save(recoveryCase)).thenReturn(recoveryCase);

        service.analyze(1L);
        service.execute(1L);

        assertEquals("AI", recoveryCase.getDecisionSource());
        assertEquals(RecoveryActionType.CREATE_PAYMENT_LINK, recoveryCase.getRecommendedAction());
        assertEquals(new BigDecimal("1043.42"), recoveryCase.getExpectedRecoveryAmount());
        assertEquals(RecoveryStatus.RECOVERED, recoveryCase.getStatus());
        verifyNoInteractions(razorpay);
    }
}
