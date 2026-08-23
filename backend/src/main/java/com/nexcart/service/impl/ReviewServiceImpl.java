package com.nexcart.service.impl;
import org.springframework.transaction.annotation.Transactional;
import com.nexcart.dto.request.ReviewRequest;
import com.nexcart.dto.response.ReviewResponse;
import com.nexcart.entity.Product;
import com.nexcart.entity.Review;
import com.nexcart.entity.User;
import com.nexcart.exception.ProductNotFoundException;
import com.nexcart.exception.ReviewAlreadyExistsException;
import com.nexcart.exception.ReviewNotFoundException;
import com.nexcart.mapper.ReviewMapper;
import com.nexcart.repository.ProductRepository;
import com.nexcart.repository.ReviewRepository;
import com.nexcart.repository.UserRepository;
import com.nexcart.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ReviewMapper reviewMapper;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found."));
    }

    @Override
    @Transactional
    public ReviewResponse addReview(Long productId, ReviewRequest request) {

        User user = getCurrentUser();

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new ProductNotFoundException("Product not found with ID: " + productId));

        if (reviewRepository.existsByUserAndProduct(user, product)) {
            throw new ReviewAlreadyExistsException("You have already reviewed this product.");
        }

        Review review = reviewMapper.toEntity(request);
        review.setUser(user);
        review.setProduct(product);
        reviewRepository.save(review);

        Review savedReview = reviewRepository.findById(review.getId())
                .orElseThrow(() ->
                        new ReviewNotFoundException("Review not found."));

        return reviewMapper.toResponse(savedReview);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsByProduct(Long productId) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new ProductNotFoundException("Product not found with ID: " + productId));

        return reviewRepository.findByProduct(product)
                .stream()
                .map(reviewMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ReviewResponse updateReview(Long reviewId, ReviewRequest request) {

        User currentUser = getCurrentUser();

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() ->
                        new ReviewNotFoundException("Review not found with ID: " + reviewId));

        if (!review.getUser().getId().equals(currentUser.getId())) {
            throw new ReviewNotFoundException("You are not authorized to update this review.");
        }
        reviewMapper.updateEntity(review, request);

        reviewRepository.save(review);

        Review updatedReview = reviewRepository.findById(reviewId)
                .orElseThrow(() ->
                        new ReviewNotFoundException("Review not found with ID: " + reviewId));

        return reviewMapper.toResponse(updatedReview);
    }

    @Override
    @Transactional
    public void deleteReview(Long reviewId) {

        User currentUser = getCurrentUser();

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() ->
                        new ReviewNotFoundException("Review not found with ID: " + reviewId));

        if (!review.getUser().getId().equals(currentUser.getId())) {
            throw new ReviewNotFoundException("You are not authorized to delete this review.");
        }

        reviewRepository.delete(review);
    }
}
