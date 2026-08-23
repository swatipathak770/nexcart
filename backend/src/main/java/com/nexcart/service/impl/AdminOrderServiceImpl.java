package com.nexcart.service.impl;

import com.nexcart.dto.response.OrderResponse;
import com.nexcart.entity.Order;
import com.nexcart.entity.OrderStatus;
import com.nexcart.exception.ResourceNotFoundException;
import com.nexcart.mapper.OrderMapper;
import com.nexcart.repository.OrderRepository;
import com.nexcart.service.AdminOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminOrderServiceImpl implements AdminOrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    @Override
    public List<OrderResponse> getAllOrders() {

        return orderRepository.findAll()
                .stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    @Override
    public OrderResponse getOrderById(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order not found"));

        return orderMapper.toResponse(order);
    }

    @Override
    public List<OrderResponse> getOrdersByStatus(OrderStatus status) {

        return orderRepository.findByStatus(status)
                .stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId,
                                           OrderStatus status) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order not found"));

        order.setStatus(status);

        Order updatedOrder = orderRepository.save(order);

        return orderMapper.toResponse(updatedOrder);
    }
}
