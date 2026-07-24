package com.nexcart.service;

import com.nexcart.dto.request.ApplyCouponRequest;
import com.nexcart.dto.request.CouponRequest;
import com.nexcart.dto.response.ApplyCouponResponse;
import com.nexcart.dto.response.CouponResponse;

import java.util.List;

public interface CouponService {

    CouponResponse createCoupon(CouponRequest request);

    CouponResponse getCouponById(Long couponId);

    List<CouponResponse> getAllCoupons();

    CouponResponse updateCoupon(Long couponId, CouponRequest request);

    void deleteCoupon(Long couponId);

    ApplyCouponResponse applyCoupon(ApplyCouponRequest request);
}
