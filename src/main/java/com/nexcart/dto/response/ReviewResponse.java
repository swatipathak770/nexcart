package com.nexcart.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponse {

    private Long reviewId;

    private Integer rating;

    private String comment;

    private Long productId;

    private String productName;

    private Long userId;

    private String userName;

}
