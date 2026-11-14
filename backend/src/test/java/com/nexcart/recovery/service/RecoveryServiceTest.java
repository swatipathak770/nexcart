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
import com.nexcart.repository.PaymentAttemptRepository;
import com.razorpay.RazorpayClient;
import com.razorpay.OrderClient;
import com.razorpay.Order;
import org.json.JSONObject;
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
    private final PaymentAttemptRepository paymentAttempts = mock(PaymentAttemptRepository.class);
    private final OrderRepository orders = mock(OrderRepository.class);
    private final RecoveryDecisionService decisions = mock(RecoveryDecisionService.class);
    private final RazorpayClient razorpay = mock(RazorpayClient.class);
    private final OrderClient razorpayOrders = mock(OrderClient.class);
    private final RecoveryService service = new RecoveryService(cases, actions, audits, payments, paymentAttempts, orders, decisions, razorpay);
    {
        ReflectionTestUtils.setField(service, "maxAttempts", 3);
        razorpay.orders = razorpayOrders;
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
    void historicalSuccessfulRetryDoesNotBlockFreshAttemptAfterPreviousAttemptEnded() throws Exception {
        com.nexcart.entity.Order order = com.nexcart.entity.Order.builder().status(com.nexcart.entity.OrderStatus.PENDING).build();
        order.setId(32L);
        com.nexcart.entity.Payment payment = com.nexcart.entity.Payment.builder().id(7L).order(order)
                .amount(new BigDecimal("699.00")).paymentStatus(com.nexcart.entity.PaymentStatus.CANCELLED).build();
        RecoveryCase recoveryCase = RecoveryCase.builder().order(order).amount(payment.getAmount())
                .status(RecoveryStatus.ACTION_RECOMMENDED).recommendedAction(RecoveryActionType.RETRY_PAYMENT).build();
        recoveryCase.setId(34L);
        RecoveryAction completedRetry = RecoveryAction.builder()
                .recoveryCase(recoveryCase)
                .action(RecoveryActionType.RETRY_PAYMENT)
                .successful(true)
                .build();
        when(cases.findByIdForUpdate(34L)).thenReturn(Optional.of(recoveryCase));
        when(orders.findById(32L)).thenReturn(Optional.of(order));
        when(payments.findByOrderId(32L)).thenReturn(Optional.of(payment));
        when(payments.findByOrderIdForUpdate(32L)).thenReturn(Optional.of(payment));
        when(paymentAttempts.existsByPaymentIdAndStatus(7L, com.nexcart.entity.PaymentStatus.PENDING)).thenReturn(false);
        when(paymentAttempts.findByPaymentIdAndStatusOrderByAttemptNumberDesc(7L, com.nexcart.entity.PaymentStatus.PENDING)).thenReturn(List.of());
        when(paymentAttempts.findByPaymentIdOrderByAttemptNumberAsc(7L)).thenReturn(List.of());
        when(razorpayOrders.create(any(JSONObject.class))).thenReturn(new Order(new JSONObject().put("id", "order_retry_second")));
        when(paymentAttempts.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(payments.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(actions.findByRecoveryCaseIdOrderByCreatedAtAsc(34L)).thenReturn(List.of(completedRetry));
        when(actions.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(cases.save(recoveryCase)).thenReturn(recoveryCase);

        service.execute(34L);

        verify(razorpayOrders).create(any(JSONObject.class));
        verify(paymentAttempts).save(argThat(attempt -> "order_retry_second".equals(attempt.getRazorpayOrderId())));
    }

    @org.junit.jupiter.api.Test
    void maximumAttemptsForceNoAction() {
        RecoveryCase recoveryCase = RecoveryCase.builder()
                .status(RecoveryStatus.ACTION_RECOMMENDED)
                .recommendedAction(RecoveryActionType.CREATE_PAYMENT_LINK)
                .actionAttempts(3)
                .build();
        when(cases.findByIdForUpdate(1L)).thenReturn(Optional.of(recoveryCase));
        when(cases.save(recoveryCase)).thenReturn(recoveryCase);

        service.execute(1L);

        assertEquals(RecoveryStatus.NO_ACTION, recoveryCase.getStatus());
        assertEquals("REJECTED: no safe recovery action available", recoveryCase.getGuardrailResult());
        verifyNoInteractions(actions, razorpay);
    }

    @org.junit.jupiter.api.Test
    void customerCancelledOrderStopsRecoveryBeforeDecisionOrAction() {
        com.nexcart.entity.Order order = com.nexcart.entity.Order.builder()
                .status(com.nexcart.entity.OrderStatus.CANCELLED)
                .build();
        RecoveryCase recoveryCase = RecoveryCase.builder()
                .order(order)
                .status(RecoveryStatus.DETECTED)
                .recommendedAction(RecoveryActionType.RETRY_PAYMENT)
                .build();
        when(cases.findByIdForUpdate(1L)).thenReturn(Optional.of(recoveryCase));
        when(orders.findById(null)).thenReturn(Optional.of(order));
        when(actions.findByRecoveryCaseIdOrderByCreatedAtAsc(null)).thenReturn(List.of());
        when(cases.save(recoveryCase)).thenReturn(recoveryCase);

        service.analyze(1L);

        assertEquals(RecoveryStatus.NO_ACTION, recoveryCase.getStatus());
        assertEquals(RecoveryActionType.NO_ACTION, recoveryCase.getRecommendedAction());
        verifyNoInteractions(decisions, razorpay);
        verify(actions).findByRecoveryCaseIdOrderByCreatedAtAsc(null);
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

    @org.junit.jupiter.api.Test
    void retryExecutionCreatesFreshAttemptForExistingPendingOrder() throws Exception {
        com.nexcart.entity.Order order = com.nexcart.entity.Order.builder().status(com.nexcart.entity.OrderStatus.PENDING)
                .finalAmount(new BigDecimal("699.00")).build();
        order.setId(32L);
        com.nexcart.entity.Payment payment = com.nexcart.entity.Payment.builder().id(7L).order(order)
                .amount(new BigDecimal("699.00")).paymentStatus(com.nexcart.entity.PaymentStatus.FAILED).build();
        RecoveryCase recoveryCase = RecoveryCase.builder().order(order).amount(payment.getAmount())
                .status(RecoveryStatus.ACTION_RECOMMENDED).recommendedAction(RecoveryActionType.RETRY_PAYMENT).build();
        recoveryCase.setId(34L);
        when(cases.findByIdForUpdate(34L)).thenReturn(Optional.of(recoveryCase));
        when(orders.findById(32L)).thenReturn(Optional.of(order));
        when(payments.findByOrderIdForUpdate(32L)).thenReturn(Optional.of(payment));
        when(paymentAttempts.findByPaymentIdAndStatusOrderByAttemptNumberDesc(7L, com.nexcart.entity.PaymentStatus.PENDING)).thenReturn(List.of());
        when(paymentAttempts.findByPaymentIdOrderByAttemptNumberAsc(7L)).thenReturn(List.of());
        when(razorpayOrders.create(any(JSONObject.class))).thenReturn(new Order(new JSONObject().put("id", "order_retry_34")));
        when(paymentAttempts.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(payments.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(actions.findByRecoveryCaseIdOrderByCreatedAtAsc(34L)).thenReturn(List.of());
        when(actions.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(cases.save(recoveryCase)).thenReturn(recoveryCase);

        service.execute(34L);

        assertEquals(RecoveryStatus.ACTION_PENDING, recoveryCase.getStatus());
        assertEquals(com.nexcart.entity.PaymentStatus.PENDING, payment.getPaymentStatus());
        assertEquals("order_retry_34", payment.getRazorpayOrderId());
        verify(paymentAttempts).save(argThat(a -> a.getAttemptNumber() == 1
                && a.getStatus() == com.nexcart.entity.PaymentStatus.PENDING
                && "order_retry_34".equals(a.getRazorpayOrderId())));
        verify(audits).save(argThat(a -> "PAYMENT_ATTEMPT_CREATED".equals(a.getEventType())));
    }

    @org.junit.jupiter.api.Test
    void repeatedRetryExecutionDoesNotCreateAnotherAttempt() throws Exception {
        RecoveryCase recoveryCase = RecoveryCase.builder().status(RecoveryStatus.ACTION_PENDING)
                .order(com.nexcart.entity.Order.builder().status(com.nexcart.entity.OrderStatus.PENDING).build())
                .recommendedAction(RecoveryActionType.RETRY_PAYMENT).executedAction(RecoveryActionType.RETRY_PAYMENT)
                .actionAttempts(1).build();
        recoveryCase.getOrder().setId(3L);
        when(cases.findByIdForUpdate(1L)).thenReturn(Optional.of(recoveryCase));
        when(orders.findById(3L)).thenReturn(Optional.of(recoveryCase.getOrder()));
        com.nexcart.entity.Payment payment = com.nexcart.entity.Payment.builder().id(9L).order(recoveryCase.getOrder())
                .paymentStatus(com.nexcart.entity.PaymentStatus.PENDING).build();
        when(payments.findByOrderId(3L)).thenReturn(Optional.of(payment));
        when(paymentAttempts.existsByPaymentIdAndStatus(9L, com.nexcart.entity.PaymentStatus.PENDING)).thenReturn(true);
        when(cases.save(recoveryCase)).thenReturn(recoveryCase);

        service.execute(1L);

        verifyNoInteractions(razorpay);
        verify(paymentAttempts, never()).save(any());
    }
}
