package com.nexcart.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplyCouponResponse {

    private String couponCode;

    private BigDecimal originalAmount;

    private BigDecimal discount;

    private BigDecimal finalAmount;

    private String message;
}