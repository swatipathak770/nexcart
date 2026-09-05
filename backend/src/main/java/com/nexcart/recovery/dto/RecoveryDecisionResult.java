package com.nexcart.recovery.dto;

public record RecoveryDecisionResult(RecoveryDecision decision, String source, String fallbackReason) { }
