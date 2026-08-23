package com.nexcart.service.impl;

import com.nexcart.dto.response.OrderResponse;
import com.nexcart.entity.*;
import com.nexcart.exception.ResourceNotFoundException;
import com.nexcart.mapper.OrderMapper;
import com.nexcart.repository.CartRepository;
import com.nexcart.repository.OrderRepository;
import com.nexcart.repository.ProductRepository;
import com.nexcart.repository.UserRepository;
import com.nexcart.service.CouponService;
import com.nexcart.service.OrderService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderMapper orderMapper;
    private final CouponService couponService;

    private User getCurrentUser() {

        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));
    }

    @Override
    @Transactional
    public OrderResponse placeOrder(String couponCode) {

        User user = getCurrentUser();

        List<Cart> cartItems = cartRepository.findByUser(user);

        if (cartItems.isEmpty()) {
            throw new RuntimeException("Cart is empty.");
        }

        BigDecimal total = BigDecimal.ZERO;

        // Calculate total amount
        for (Cart cart : cartItems) {

            Product product = cart.getProduct();

            if (product.getStock() < cart.getQuantity()) {
                throw new RuntimeException(product.getName() + " is out of stock.");
            }

            total = total.add(
                    product.getPrice()
                            .multiply(BigDecimal.valueOf(cart.getQuantity()))
            );
        }

        BigDecimal discount = BigDecimal.ZERO;
        Coupon appliedCoupon = null;

        if (couponCode != null && !couponCode.isBlank()) {

            appliedCoupon = couponService.validateCoupon(couponCode, total, user);

            if (appliedCoupon.getDiscountType() == DiscountType.PERCENTAGE) {

                discount = total
                        .multiply(appliedCoupon.getDiscountValue())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                if (appliedCoupon.getMaximumDiscount() != null
                        && discount.compareTo(appliedCoupon.getMaximumDiscount()) > 0) {

                    discount = appliedCoupon.getMaximumDiscount();
                }

            } else {

                discount = appliedCoupon.getDiscountValue();
            }
        }

        BigDecimal finalAmount = total.subtract(discount);

        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }

        Order order = Order.builder()
                .user(user)
                .status(OrderStatus.PENDING)
                .totalAmount(total)
                .discountAmount(discount)
                .finalAmount(finalAmount)
                .coupon(appliedCoupon)
                .build();

        // Create order items and reduce stock
        for (Cart cart : cartItems) {

            Product product = cart.getProduct();

            product.setStock(product.getStock() - cart.getQuantity());
            productRepository.save(product);

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(cart.getQuantity())
                    .price(product.getPrice())
                    .build();

            order.getOrderItems().add(orderItem);
        }

        Order savedOrder = orderRepository.save(order);

        if (appliedCoupon != null) {
            couponService.recordUsage(appliedCoupon, user, savedOrder);
        }

        cartRepository.deleteByUser(user);

        return orderMapper.toResponse(savedOrder);
    }

    @Override
    public List<OrderResponse> getMyOrders() {

        User user = getCurrentUser();

        return orderRepository.findByUser(user)
                .stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    @Override
    public OrderResponse getOrderById(Long orderId) {

        User user = getCurrentUser();

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order not found"));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You are not authorized to view this order.");
        }

        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional
    public void cancelOrder(Long orderId) {

        User user = getCurrentUser();

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order not found"));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You are not authorized to cancel this order.");
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new RuntimeException("Order is already cancelled.");
        }

        for (OrderItem item : order.getOrderItems()) {

            Product product = item.getProduct();

            product.setStock(product.getStock() + item.getQuantity());

            productRepository.save(product);
        }

        order.setStatus(OrderStatus.CANCELLED);

        orderRepository.save(order);
    }
}
