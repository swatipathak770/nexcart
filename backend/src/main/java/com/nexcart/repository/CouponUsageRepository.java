package com.nexcart.repository;

import com.nexcart.entity.Coupon;
import com.nexcart.entity.CouponUsage;
import com.nexcart.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponUsageRepository extends JpaRepository<CouponUsage, Long> {
    boolean existsByCouponAndUser(Coupon coupon, User user);
}
