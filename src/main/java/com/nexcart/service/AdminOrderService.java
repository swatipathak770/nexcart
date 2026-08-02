package com.nexcart.service;

import com.nexcart.dto.response.OrderResponse;
import com.nexcart.entity.OrderStatus;

import java.util.List;

public interface AdminOrderService {


    List<OrderResponse> getAllOrders();


    OrderResponse getOrderById(Long orderId);


    List<OrderResponse> getOrdersByStatus(OrderStatus status);


    OrderResponse updateOrderStatus(Long orderId, OrderStatus status);
}
