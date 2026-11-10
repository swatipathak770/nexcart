package com.nexcart.service;

import com.nexcart.dto.response.ReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AdminReviewService {

    Page<ReviewResponse> getAllReviews(Pageable pageable);

    ReviewResponse getReviewById(Long reviewId);

    void deleteReview(Long reviewId);
}
