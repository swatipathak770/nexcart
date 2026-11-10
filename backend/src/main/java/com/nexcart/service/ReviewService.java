package com.nexcart.service;

import com.nexcart.dto.request.ReviewRequest;
import com.nexcart.dto.response.ReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ReviewService {

    ReviewResponse addReview(Long productId, ReviewRequest request);

    Page<ReviewResponse> getReviewsByProduct(Long productId, Pageable pageable);

    ReviewResponse updateReview(Long reviewId, ReviewRequest request);

    void deleteReview(Long reviewId);

}
