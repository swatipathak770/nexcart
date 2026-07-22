package com.nexcart.mapper;

import com.nexcart.dto.response.OrderItemResponse;
import com.nexcart.dto.response.OrderResponse;
import com.nexcart.entity.Order;
import com.nexcart.entity.OrderItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class OrderMapper {

    public OrderResponse toResponse(Order order) {

        List<OrderItemResponse> items = order.getOrderItems()
                .stream()
                .map(this::toItemResponse)
                .toList();

        return OrderResponse.builder()
                .orderId(order.getId())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .items(items)
                .build();
    }

    private OrderItemResponse toItemResponse(OrderItem item) {

        BigDecimal subtotal = item.getPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));

        return OrderItemResponse.builder()
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .imageUrl(item.getProduct().getImageUrl())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .subtotal(subtotal)
                .build();
    }
}
