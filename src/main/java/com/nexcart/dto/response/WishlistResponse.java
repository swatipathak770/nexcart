package com.nexcart.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WishlistResponse {

    private Long wishlistId;

    private Long productId;

    private String productName;

    private String imageUrl;

    private BigDecimal price;

    private String categoryName;

    private String brandName;
}
