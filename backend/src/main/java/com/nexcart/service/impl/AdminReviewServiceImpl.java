package com.nexcart.service.impl;

import com.nexcart.dto.response.ReviewResponse;
import com.nexcart.entity.Review;
import com.nexcart.exception.ResourceNotFoundException;
import com.nexcart.mapper.ReviewMapper;
import com.nexcart.repository.ReviewRepository;
import com.nexcart.service.AdminReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminReviewServiceImpl implements AdminReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewMapper reviewMapper;

    @Override
    public List<ReviewResponse> getAllReviews() {

        return reviewRepository.findAll()
                .stream()
                .map(reviewMapper::toResponse)
                .toList();
    }

    @Override
    public ReviewResponse getReviewById(Long reviewId) {

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Review not found."));

        return reviewMapper.toResponse(review);
    }

    @Override
    public void deleteReview(Long reviewId) {

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Review not found."));

        reviewRepository.delete(review);
    }
}
