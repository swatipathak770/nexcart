package com.nexcart.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplyCouponRequest {

    @NotBlank(message = "Coupon code is required.")
    private String couponCode;

    @NotNull(message = "Order amount is required.")
    @DecimalMin(value = "0.01", message = "Order amount must be greater than zero.")
    private BigDecimal orderAmount;
}
