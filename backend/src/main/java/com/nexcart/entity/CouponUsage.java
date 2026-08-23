package com.nexcart.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "coupon_usages", uniqueConstraints = @UniqueConstraint(columnNames = {"coupon_id", "user_id"}))
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponUsage extends BaseEntity {

    @ManyToOne(optional = false)
    private Coupon coupon;

    @ManyToOne(optional = false)
    private User user;

    @ManyToOne(optional = false)
    private Order order;
}
