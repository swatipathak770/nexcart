package com.nexcart.dto.response;

import lombok.Builder;

@Builder
public record OrderDeliveryAddressResponse(
        String fullName,
        String phoneNumber,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String country,
        String postalCode) {
}
