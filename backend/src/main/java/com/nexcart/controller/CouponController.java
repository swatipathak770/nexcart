package com.nexcart.controller;

import com.nexcart.dto.request.ApplyCouponRequest;
import com.nexcart.dto.request.CouponRequest;
import com.nexcart.dto.response.ApplyCouponResponse;
import com.nexcart.dto.response.CouponResponse;
import com.nexcart.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
@Tag(name = "Coupon Controller", description = "APIs for managing and applying coupons")
public class CouponController {

    private final CouponService couponService;

    @Operation(summary = "Create a new coupon")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<CouponResponse> createCoupon(
            @Valid @RequestBody CouponRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(couponService.createCoupon(request));
    }

    @Operation(summary = "Get coupon by ID")
    @GetMapping("/{couponId}")
    public ResponseEntity<CouponResponse> getCouponById(
            @PathVariable Long couponId) {

        return ResponseEntity.ok(couponService.getCouponById(couponId));
    }

    @Operation(summary = "Get all coupons")
    @GetMapping
    public ResponseEntity<List<CouponResponse>> getAllCoupons() {

        return ResponseEntity.ok(couponService.getAllCoupons());
    }

    @Operation(summary = "Update coupon")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{couponId}")
    public ResponseEntity<CouponResponse> updateCoupon(
            @PathVariable Long couponId,
            @Valid @RequestBody CouponRequest request) {

        return ResponseEntity.ok(
                couponService.updateCoupon(couponId, request));
    }

    @Operation(summary = "Delete coupon")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{couponId}")
    public ResponseEntity<String> deleteCoupon(
            @PathVariable Long couponId) {

        couponService.deleteCoupon(couponId);

        return ResponseEntity.ok("Coupon deleted successfully.");
    }

    @Operation(summary = "Apply coupon")
    @PostMapping("/apply")
    public ResponseEntity<ApplyCouponResponse> applyCoupon(
            @Valid @RequestBody ApplyCouponRequest request) {

        return ResponseEntity.ok(couponService.applyCoupon(request));
    }
}