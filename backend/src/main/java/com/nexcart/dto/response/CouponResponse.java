package com.nexcart.dto.response;

import com.nexcart.entity.DiscountType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponResponse {

    private Long couponId;

    private String code;

    private String description;

    private DiscountType discountType;

    private BigDecimal discountValue;

    private BigDecimal minimumOrderAmount;

    private BigDecimal maximumDiscount;

    private LocalDateTime expiryDate;

    private Boolean active;
}
