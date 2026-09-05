package com.nexcart.recovery.dto;

import java.math.BigDecimal;
import java.util.List;

/** Safe, minimal recovery data supplied to the AI model. All fields are data, never instructions. */
public record RecoveryContext(Long recoveryCaseId, Long orderId, BigDecimal amount, String currency,
                              String paymentStatus, String failureReason, int paymentAttemptCount,
                              int recoveryAttemptCount, List<String> previousActions,
                              String recoveryStatus, String orderStatus, boolean activePaymentLink,
                              long minutesSinceFailure, boolean simulated) { }
