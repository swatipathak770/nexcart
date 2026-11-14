package com.nexcart.dto.response;

import java.math.BigDecimal;

/** Public Razorpay Checkout data for the authenticated owner of an order. */
public record PaymentCheckoutResponse(
        Long orderId,
        String razorpayOrderId,
        String keyId,
        BigDecimal amount,
        String currency) {
}
