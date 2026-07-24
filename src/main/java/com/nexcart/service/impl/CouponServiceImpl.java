package com.nexcart.service.impl;

import com.nexcart.dto.request.ApplyCouponRequest;
import com.nexcart.dto.request.CouponRequest;
import com.nexcart.dto.response.ApplyCouponResponse;
import com.nexcart.dto.response.CouponResponse;
import com.nexcart.entity.Coupon;
import com.nexcart.entity.DiscountType;
import com.nexcart.exception.CouponAlreadyExistsException;
import com.nexcart.exception.CouponNotFoundException;
import com.nexcart.exception.InvalidCouponException;
import com.nexcart.mapper.CouponMapper;
import com.nexcart.repository.CouponRepository;
import com.nexcart.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final CouponMapper couponMapper;

    @Override
    @Transactional
    public CouponResponse createCoupon(CouponRequest request) {

        if (couponRepository.existsByCode(request.getCode())) {
            throw new CouponAlreadyExistsException(
                    "Coupon with code '" + request.getCode() + "' already exists.");
        }

        Coupon coupon = couponMapper.toEntity(request);

        couponRepository.save(coupon);

        Coupon savedCoupon = couponRepository.findById(coupon.getId())
                .orElseThrow(() ->
                        new CouponNotFoundException("Coupon not found."));

        return couponMapper.toResponse(savedCoupon);
    }

    @Override
    @Transactional(readOnly = true)
    public CouponResponse getCouponById(Long couponId) {

        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() ->
                        new CouponNotFoundException(
                                "Coupon not found with ID: " + couponId));

        return couponMapper.toResponse(coupon);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CouponResponse> getAllCoupons() {

        return couponRepository.findAll()
                .stream()
                .map(couponMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public CouponResponse updateCoupon(Long couponId, CouponRequest request) {

        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() ->
                        new CouponNotFoundException(
                                "Coupon not found with ID: " + couponId));

        if (!coupon.getCode().equals(request.getCode())
                && couponRepository.existsByCode(request.getCode())) {

            throw new CouponAlreadyExistsException(
                    "Coupon with code '" + request.getCode() + "' already exists.");
        }

        couponMapper.updateEntity(coupon, request);

        couponRepository.save(coupon);

        Coupon updatedCoupon = couponRepository.findById(couponId)
                .orElseThrow(() ->
                        new CouponNotFoundException(
                                "Coupon not found with ID: " + couponId));

        return couponMapper.toResponse(updatedCoupon);
    }

    @Override
    @Transactional
    public void deleteCoupon(Long couponId) {

        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() ->
                        new CouponNotFoundException(
                                "Coupon not found with ID: " + couponId));

        couponRepository.delete(coupon);
    }

    @Override
    @Transactional(readOnly = true)
    public ApplyCouponResponse applyCoupon(ApplyCouponRequest request) {

        Coupon coupon = couponRepository.findByCodeIgnoreCase(request.getCouponCode())
                .orElseThrow(() ->
                        new CouponNotFoundException(
                                "Coupon not found with code: " + request.getCouponCode()));

        if (!coupon.getActive()) {
            throw new InvalidCouponException("Coupon is inactive.");
        }

        if (coupon.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new InvalidCouponException("Coupon has expired.");
        }

        BigDecimal orderAmount = request.getOrderAmount();

        if (orderAmount.compareTo(coupon.getMinimumOrderAmount()) < 0) {
            throw new InvalidCouponException(
                    "Minimum order amount should be ₹" + coupon.getMinimumOrderAmount());
        }

        BigDecimal discount;

        if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {

            discount = orderAmount
                    .multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            if (coupon.getMaximumDiscount() != null
                    && discount.compareTo(coupon.getMaximumDiscount()) > 0) {

                discount = coupon.getMaximumDiscount();
            }

        } else {

            discount = coupon.getDiscountValue();
        }

        BigDecimal finalAmount = orderAmount.subtract(discount);

        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }

        return ApplyCouponResponse.builder()
                .couponCode(coupon.getCode())
                .originalAmount(orderAmount)
                .discount(discount)
                .finalAmount(finalAmount)
                .message("Coupon applied successfully.")
                .build();
    }
}
