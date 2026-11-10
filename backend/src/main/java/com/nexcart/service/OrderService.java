package com.nexcart.service;

import com.nexcart.dto.response.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OrderService {

    OrderResponse placeOrder(String couponCode);

    Page<OrderResponse> getMyOrders(Pageable pageable);

    OrderResponse getOrderById(Long orderId);

    void cancelOrder(Long orderId);
}
