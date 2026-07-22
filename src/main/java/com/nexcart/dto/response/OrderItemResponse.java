package com.nexcart.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemResponse {

    private Long productId;

    private String productName;

    private String imageUrl;

    private Integer quantity;

    private BigDecimal price;

    private BigDecimal subtotal;
}
