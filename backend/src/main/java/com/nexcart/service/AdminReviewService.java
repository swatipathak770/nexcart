package com.nexcart.service;

import com.nexcart.dto.response.ReviewResponse;

import java.util.List;

public interface AdminReviewService {

    List<ReviewResponse> getAllReviews();

    ReviewResponse getReviewById(Long reviewId);

    void deleteReview(Long reviewId);
}
