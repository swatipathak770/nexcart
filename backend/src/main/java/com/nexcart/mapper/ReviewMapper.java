package com.nexcart.mapper;

import com.nexcart.dto.request.ReviewRequest;
import com.nexcart.dto.response.ReviewResponse;
import com.nexcart.entity.Review;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {

    public Review toEntity(ReviewRequest request) {
        if (request == null) {
            return null;
        }

        return Review.builder()
                .rating(request.getRating())
                .comment(request.getComment())
                .build();
    }

    public ReviewResponse toResponse(Review review) {
        if (review == null) {
            return null;
        }

        return ReviewResponse.builder()
                .reviewId(review.getId())
                .rating(review.getRating())
                .comment(review.getComment())
                .productId(review.getProduct().getId())
                .productName(review.getProduct().getName())
                .userId(review.getUser().getId())
                .userName(review.getUser().getFullName())
                .build();
    }

    public void updateEntity(Review review, ReviewRequest request) {
        review.setRating(request.getRating());
        review.setComment(request.getComment());
    }
}