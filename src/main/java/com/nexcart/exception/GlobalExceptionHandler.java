package com.nexcart.exception;

import com.nexcart.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Object>> handleEmailAlreadyExists(
            EmailAlreadyExistsException ex) {

        ApiResponse<Object> response =
                ApiResponse.builder()
                        .success(false)
                        .message(ex.getMessage())
                        .data(null)
                        .timestamp(LocalDateTime.now())
                        .build();

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }
    @ExceptionHandler(CategoryAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Object>> handleCategoryAlreadyExists(
            CategoryAlreadyExistsException ex) {

        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }
    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleCategoryNotFound(
            CategoryNotFoundException ex) {

        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
    @ExceptionHandler(BrandAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Object>> handleBrandAlreadyExists(
            BrandAlreadyExistsException ex) {

        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }
    @ExceptionHandler(BrandNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleBrandNotFound(
            BrandNotFoundException ex) {

        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
    @ExceptionHandler(ProductAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Object>> handleProductAlreadyExists(
            ProductAlreadyExistsException ex) {

        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleProductNotFound(
            ProductNotFoundException ex) {

        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
    @ExceptionHandler(WishlistAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Object>> handleWishlistAlreadyExists(
            WishlistAlreadyExistsException ex) {

        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(WishlistNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleWishlistNotFound(
            WishlistNotFoundException ex) {

        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
    @ExceptionHandler(ReviewAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Object>> handleReviewAlreadyExistsException(
            ReviewAlreadyExistsException ex) {

        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }
    @ExceptionHandler(ReviewNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleReviewNotFoundException(
            ReviewNotFoundException ex) {

        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }
    @ExceptionHandler(CouponNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleCouponNotFoundException(
            CouponNotFoundException ex) {

        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
    @ExceptionHandler(CouponAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Object>> handleCouponAlreadyExistsException(
            CouponAlreadyExistsException ex) {

        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }
    @ExceptionHandler(InvalidCouponException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidCouponException(
            InvalidCouponException ex) {

        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadCredentials(
            BadCredentialsException ex) {

        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message("Invalid email or password")
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(response);
    }
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleUsernameNotFound(
            UsernameNotFoundException ex) {

        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message("Invalid email or password")
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(response);
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(
            MethodArgumentNotValidException ex) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Validation failed");

        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message(message)
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgument(
            IllegalArgumentException ex) {

        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleUserNotFoundException(
            UserNotFoundException ex) {

        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

}
