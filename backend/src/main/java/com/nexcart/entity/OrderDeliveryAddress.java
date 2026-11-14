package com.nexcart.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDeliveryAddress {
    @Column(name = "delivery_full_name")
    private String fullName;

    @Column(name = "delivery_phone_number", length = 10)
    private String phoneNumber;

    @Column(name = "delivery_address_line1")
    private String addressLine1;

    @Column(name = "delivery_address_line2")
    private String addressLine2;

    @Column(name = "delivery_city")
    private String city;

    @Column(name = "delivery_state")
    private String state;

    @Column(name = "delivery_country")
    private String country;

    @Column(name = "delivery_postal_code")
    private String postalCode;
}
