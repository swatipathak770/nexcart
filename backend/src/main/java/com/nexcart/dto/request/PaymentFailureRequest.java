package com.nexcart.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PaymentFailureRequest {

    @NotNull(message = "Order ID is required")
    private Long orderId;

    @Size(max = 500, message = "Failure reason must not exceed 500 characters")
    private String reason;
}
