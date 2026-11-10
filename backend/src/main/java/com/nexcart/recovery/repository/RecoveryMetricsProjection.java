package com.nexcart.recovery.repository;

import java.math.BigDecimal;

public interface RecoveryMetricsProjection {
    BigDecimal getRevenueAtRisk();
    BigDecimal getRevenueRecovered();
    BigDecimal getExpectedRecoverable();
    Long getRecoveredCases();
    Long getTotalCases();
}
