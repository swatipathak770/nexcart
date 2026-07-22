package com.nexcart.service;

import com.nexcart.dto.response.OrderResponse;

import java.util.List;

public interface OrderService {

    OrderResponse placeOrder();

    List<OrderResponse> getMyOrders();

    OrderResponse getOrderById(Long orderId);

    void cancelOrder(Long orderId);
}
