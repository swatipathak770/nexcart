package com.nexcart.service;

import com.nexcart.dto.response.OrderResponse;
import com.nexcart.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AdminOrderService {


    Page<OrderResponse> getAllOrders(Pageable pageable);


    OrderResponse getOrderById(Long orderId);


    Page<OrderResponse> getOrdersByStatus(OrderStatus status, Pageable pageable);


    OrderResponse updateOrderStatus(Long orderId, OrderStatus status);
}
