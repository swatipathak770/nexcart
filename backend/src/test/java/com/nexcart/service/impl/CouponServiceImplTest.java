package com.nexcart.service.impl;

import com.nexcart.entity.Coupon;
import com.nexcart.entity.DiscountType;
import com.nexcart.entity.User;
import com.nexcart.exception.InvalidCouponException;
import com.nexcart.mapper.CouponMapper;
import com.nexcart.repository.CouponRepository;
import com.nexcart.repository.CouponUsageRepository;
import com.nexcart.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CouponServiceImplTest {
    private final CouponRepository coupons = mock(CouponRepository.class);
    private final CouponUsageRepository usages = mock(CouponUsageRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final CouponServiceImpl service = new CouponServiceImpl(coupons, new CouponMapper(), usages, users);
    private final User user = User.builder().id(10L).email("customer@example.test").build();

    @BeforeEach
    void authenticateCustomer() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getEmail(), null));
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void percentageDiscountHonorsMaximumDiscount() {
        Coupon coupon = coupon("SAVE20", DiscountType.PERCENTAGE, "20.00", "15.00");
        when(coupons.findByCodeIgnoreCase("SAVE20")).thenReturn(Optional.of(coupon));
        when(usages.existsByCouponAndUser(coupon, user)).thenReturn(false);

        var result = service.applyCoupon(com.nexcart.dto.request.ApplyCouponRequest.builder()
                .couponCode("SAVE20").orderAmount(new BigDecimal("200.00")).build());

        assertEquals(new BigDecimal("15.00"), result.getDiscount());
        assertEquals(new BigDecimal("185.00"), result.getFinalAmount());
        verify(usages).existsByCouponAndUser(coupon, user);
    }

    @Test
    void flatDiscountNeverProducesNegativeOrderTotal() {
        Coupon coupon = coupon("FLAT500", DiscountType.FLAT, "500.00", null);
        when(coupons.findByCodeIgnoreCase("FLAT500")).thenReturn(Optional.of(coupon));
        when(usages.existsByCouponAndUser(coupon, user)).thenReturn(false);

        var result = service.applyCoupon(com.nexcart.dto.request.ApplyCouponRequest.builder()
                .couponCode("FLAT500").orderAmount(new BigDecimal("100.00")).build());

        assertEquals(new BigDecimal("500.00"), result.getDiscount());
        assertEquals(BigDecimal.ZERO, result.getFinalAmount());
    }

    @Test
    void expiredInactiveAndBelowMinimumCouponsAreRejected() {
        Coupon expired = coupon("OLD", DiscountType.FLAT, "10.00", null);
        expired.setExpiryDate(LocalDateTime.now().minusDays(1));
        when(coupons.findByCodeIgnoreCase("OLD")).thenReturn(Optional.of(expired));
        assertThrows(InvalidCouponException.class,
                () -> service.validateCoupon("OLD", new BigDecimal("100.00"), null));

        Coupon inactive = coupon("OFF", DiscountType.FLAT, "10.00", null);
        inactive.setActive(false);
        when(coupons.findByCodeIgnoreCase("OFF")).thenReturn(Optional.of(inactive));
        assertThrows(InvalidCouponException.class,
                () -> service.validateCoupon("OFF", new BigDecimal("100.00"), null));

        Coupon minimum = coupon("MIN", DiscountType.FLAT, "10.00", null);
        when(coupons.findByCodeIgnoreCase("MIN")).thenReturn(Optional.of(minimum));
        assertThrows(InvalidCouponException.class,
                () -> service.validateCoupon("MIN", new BigDecimal("50.00"), null));
    }

    @Test
    void customerCannotReuseCoupon() {
        Coupon coupon = coupon("ONCE", DiscountType.FLAT, "10.00", null);
        when(coupons.findByCodeIgnoreCase("ONCE")).thenReturn(Optional.of(coupon));
        when(usages.existsByCouponAndUser(coupon, user)).thenReturn(true);

        assertThrows(InvalidCouponException.class,
                () -> service.validateCoupon("ONCE", new BigDecimal("100.00"), user));
    }

    private Coupon coupon(String code, DiscountType type, String value, String maxDiscount) {
        return Coupon.builder()
                .code(code)
                .description("Coupon test")
                .discountType(type)
                .discountValue(new BigDecimal(value))
                .minimumOrderAmount(new BigDecimal("75.00"))
                .maximumDiscount(maxDiscount == null ? null : new BigDecimal(maxDiscount))
                .expiryDate(LocalDateTime.now().plusDays(3))
                .active(true)
                .build();
    }
}
