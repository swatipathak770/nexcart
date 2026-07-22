package com.nexcart.service.impl;

import com.nexcart.dto.response.OrderResponse;
import com.nexcart.mapper.OrderMapper;
import com.nexcart.repository.*;
import com.nexcart.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderMapper orderMapper;

    @Override
    public OrderResponse placeOrder() {
        return null;
    }

    @Override
    public List<OrderResponse> getMyOrders() {
        return List.of();
    }

    @Override
    public OrderResponse getOrderById(Long orderId) {
        return null;
    }

    @Override
    public void cancelOrder(Long orderId) {

    }
}
