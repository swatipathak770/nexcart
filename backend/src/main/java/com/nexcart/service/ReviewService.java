package com.nexcart.service;

import com.nexcart.dto.request.ReviewRequest;
import com.nexcart.dto.response.ReviewResponse;

import java.util.List;

public interface ReviewService {

    ReviewResponse addReview(Long productId, ReviewRequest request);

    List<ReviewResponse> getReviewsByProduct(Long productId);

    ReviewResponse updateReview(Long reviewId, ReviewRequest request);

    void deleteReview(Long reviewId);

}
