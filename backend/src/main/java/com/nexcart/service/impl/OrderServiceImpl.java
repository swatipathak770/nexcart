package com.nexcart.service.impl;

import com.nexcart.dto.response.OrderResponse;
import com.nexcart.entity.*;
import com.nexcart.exception.ResourceNotFoundException;
import com.nexcart.mapper.OrderMapper;
import com.nexcart.repository.CartRepository;
import com.nexcart.repository.OrderRepository;
import com.nexcart.repository.ProductRepository;
import com.nexcart.repository.UserRepository;
import com.nexcart.repository.PaymentRepository;
import com.nexcart.repository.PaymentAttemptRepository;
import com.nexcart.repository.CheckoutIdempotencyKeyRepository;
import com.nexcart.repository.AddressRepository;
import com.nexcart.service.CouponService;
import com.nexcart.service.OrderService;
import com.nexcart.recovery.service.RecoveryService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
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
    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final CheckoutIdempotencyKeyRepository checkoutIdempotencyKeys;
    private final AddressRepository addressRepository;

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
    public OrderResponse placeOrder(String couponCode, String idempotencyKey) {
        return placeOrder(couponCode, idempotencyKey, null);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"products", "productDetails"}, allEntries = true)
    public OrderResponse placeOrder(String couponCode, String idempotencyKey, Long addressId) {

        User authenticatedUser = getCurrentUser();
        User user = userRepository.findByIdForUpdate(authenticatedUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        CheckoutIdempotencyKey priorRequest = checkoutIdempotencyKeys
                .findByUserAndKeyForUpdate(user.getId(), idempotencyKey)
                .orElse(null);
        if (priorRequest != null) {
            if (priorRequest.getOrder() == null) {
                throw new IllegalStateException("The checkout request is still being processed. Retry with the same idempotency key.");
            }
            return orderMapper.toResponse(priorRequest.getOrder());
        }

        OrderDeliveryAddress deliveryAddress = null;
        if (addressId != null) {
            Address address = addressRepository.findByIdAndUser(addressId, user)
                    .orElseThrow(() -> new ResourceNotFoundException("Delivery address not found."));
            deliveryAddress = OrderDeliveryAddress.builder()
                    .fullName(address.getFullName())
                    .phoneNumber(address.getPhoneNumber())
                    .addressLine1(address.getAddressLine1())
                    .addressLine2(address.getAddressLine2())
                    .city(address.getCity())
                    .state(address.getState())
                    .country(address.getCountry())
                    .postalCode(address.getPostalCode())
                    .build();
        }

        CheckoutIdempotencyKey request = new CheckoutIdempotencyKey();
        request.setUser(user);
        request.setIdempotencyKey(idempotencyKey);
        checkoutIdempotencyKeys.saveAndFlush(request);

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
                .deliveryAddress(deliveryAddress)
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
        request.setOrder(savedOrder);
        checkoutIdempotencyKeys.save(request);

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

        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order not found"));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You are not authorized to cancel this order.");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException(order.getStatus() == OrderStatus.CANCELLED
                    ? "Order is already cancelled."
                    : "Only pending orders can be cancelled.");
        }

        Payment payment = paymentRepository.findByOrderIdForUpdate(orderId).orElse(null);
        if (payment != null && payment.getPaymentStatus() == PaymentStatus.SUCCESS) {
            throw new IllegalStateException("A successfully paid order cannot be cancelled through this flow.");
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

        if (payment != null && payment.getPaymentStatus() == PaymentStatus.PENDING) {
            payment.setPaymentStatus(PaymentStatus.CANCELLED);
            payment.setFailureReason("Order cancelled by customer.");
            paymentAttemptRepository.findByPaymentIdAndStatusForUpdate(payment.getId(), PaymentStatus.PENDING)
                    .forEach(attempt -> {
                        attempt.setStatus(PaymentStatus.CANCELLED);
                        attempt.setFailureReason("Order cancelled by customer.");
                        paymentAttemptRepository.save(attempt);
                    });
            paymentRepository.save(payment);
        }

        orderRepository.save(order);
        recoveryService.cancelForOrder(order, "Customer cancelled this order.");
    }

    private OrderResponse withRecoveryLink(Order order) {
        recoveryService.syncRecoveryForOrder(order);
        Order currentOrder = orderRepository.findById(order.getId()).orElse(order);
        OrderResponse response = orderMapper.toResponse(currentOrder);
        recoveryService.activePaymentLinkFor(currentOrder).ifPresent(response::setRecoveryPaymentLink);
        response.setRetryAvailable(recoveryService.retryCheckoutAvailable(currentOrder));
        return response;
    }
}
