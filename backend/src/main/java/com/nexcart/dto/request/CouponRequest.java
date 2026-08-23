package com.nexcart.dto.request;

import com.nexcart.entity.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponRequest {

    @NotBlank(message = "Coupon code is required.")
    private String code;

    @NotBlank(message = "Description is required.")
    private String description;

    @NotNull(message = "Discount type is required.")
    private DiscountType discountType;

    @NotNull(message = "Discount value is required.")
    @DecimalMin(value = "0.01", message = "Discount value must be greater than 0.")
    private BigDecimal discountValue;

    @NotNull(message = "Minimum order amount is required.")
    @DecimalMin(value = "0.00", message = "Minimum order amount cannot be negative.")
    private BigDecimal minimumOrderAmount;

    private BigDecimal maximumDiscount;

    @NotNull(message = "Expiry date is required.")
    @Future(message = "Expiry date must be in the future.")
    private LocalDateTime expiryDate;

    private Boolean active;
}
