package com.nexcart.dto.response;


import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartResponse {

    private Long cartId;

    private Long productId;

    private String productName;

    private String imageUrl;

    private BigDecimal price;

    private Integer quantity;

    private BigDecimal subtotal;
}
