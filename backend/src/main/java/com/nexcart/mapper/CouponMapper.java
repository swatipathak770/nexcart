package com.nexcart.mapper;

import com.nexcart.dto.request.CouponRequest;
import com.nexcart.dto.response.CouponResponse;
import com.nexcart.entity.Coupon;
import org.springframework.stereotype.Component;

@Component
public class CouponMapper {

    public Coupon toEntity(CouponRequest request) {

        return Coupon.builder()
                .code(request.getCode())
                .description(request.getDescription())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .minimumOrderAmount(request.getMinimumOrderAmount())
                .maximumDiscount(request.getMaximumDiscount())
                .expiryDate(request.getExpiryDate())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();
    }

    public CouponResponse toResponse(Coupon coupon) {

        return CouponResponse.builder()
                .couponId(coupon.getId())
                .code(coupon.getCode())
                .description(coupon.getDescription())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .minimumOrderAmount(coupon.getMinimumOrderAmount())
                .maximumDiscount(coupon.getMaximumDiscount())
                .expiryDate(coupon.getExpiryDate())
                .active(coupon.getActive())
                .build();
    }

    public void updateEntity(Coupon coupon, CouponRequest request) {

        coupon.setCode(request.getCode());
        coupon.setDescription(request.getDescription());
        coupon.setDiscountType(request.getDiscountType());
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setMinimumOrderAmount(request.getMinimumOrderAmount());
        coupon.setMaximumDiscount(request.getMaximumDiscount());
        coupon.setExpiryDate(request.getExpiryDate());

        if (request.getActive() != null) {
            coupon.setActive(request.getActive());
        }
    }
}
