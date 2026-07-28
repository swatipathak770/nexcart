package com.nexcart.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentVerificationRequest {

    @NotNull(message = "Order ID is required")
    private Long orderId;

    @NotBlank(message = "Razorpay Order ID is required")
    private String razorpayOrderId;

    @NotBlank(message = "Razorpay Payment ID is required")
    private String razorpayPaymentId;

    @NotBlank(message = "Razorpay Signature is required")
    private String razorpaySignature;
}
