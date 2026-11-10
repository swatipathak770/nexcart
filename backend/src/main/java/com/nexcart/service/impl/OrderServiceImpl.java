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
import com.nexcart.recovery.service.RecoveryService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheEvict;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderMapper orderMapper;
    private final CouponService couponService;
    private final RecoveryService recoveryService;

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
    @CacheEvict(cacheNames = {"products", "productDetails"}, allEntries = true)
    public OrderResponse placeOrder(String couponCode) {

        User user = getCurrentUser();

        // Keep product rows as proxies until findByIdForUpdate acquires the lock;
        // preloading product state with the cart can leave a stale stock value
        // while a concurrent checkout waits for that lock.
        List<Cart> cartItems = new ArrayList<>(cartRepository.findByUserForCheckout(user));

        if (cartItems.isEmpty()) {
            throw new RuntimeException("Cart is empty.");
        }

        // Lock product rows in a stable order so concurrent checkouts cannot oversell
        // and competing multi-item checkouts do not acquire locks in opposite orders.
        cartItems.sort(Comparator.comparing(cart -> cart.getProduct().getId()));
        Map<Long, Product> lockedProducts = new HashMap<>();
        for (Cart cart : cartItems) {
            Long productId = cart.getProduct().getId();
            Product lockedProduct = productRepository.findByIdForUpdate(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
            lockedProducts.put(productId, lockedProduct);
        }

        BigDecimal total = BigDecimal.ZERO;

        // Calculate total amount
        for (Cart cart : cartItems) {

            Product product = lockedProducts.get(cart.getProduct().getId());

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

            Product product = lockedProducts.get(cart.getProduct().getId());

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
    public Page<OrderResponse> getMyOrders(Pageable pageable) {

        User user = getCurrentUser();

        return orderRepository.findByUser(user, pageable).map(this::withRecoveryLink);
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

        return withRecoveryLink(order);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"products", "productDetails"}, allEntries = true)
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

        List<OrderItem> orderItems = order.getOrderItems().stream()
                .sorted(Comparator.comparing(item -> item.getProduct().getId()))
                .toList();
        for (OrderItem item : orderItems) {

            Product product = productRepository.findByIdForUpdate(item.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

            product.setStock(product.getStock() + item.getQuantity());

            productRepository.save(product);
        }

        order.setStatus(OrderStatus.CANCELLED);

        orderRepository.save(order);
        recoveryService.cancelForOrder(order, "Customer cancelled this order.");
    }

    private OrderResponse withRecoveryLink(Order order) {
        recoveryService.syncRecoveryForOrder(order);
        Order currentOrder = orderRepository.findById(order.getId()).orElse(order);
        OrderResponse response = orderMapper.toResponse(currentOrder);
        recoveryService.activePaymentLinkFor(currentOrder).ifPresent(response::setRecoveryPaymentLink);
        return response;
    }
}
