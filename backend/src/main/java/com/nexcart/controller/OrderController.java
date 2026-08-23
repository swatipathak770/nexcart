package com.nexcart.controller;

import com.nexcart.dto.response.OrderResponse;
import com.nexcart.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Order", description = "Order Management APIs")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Place an Order")
    public ResponseEntity<OrderResponse> placeOrder(
            @RequestParam(required = false) String couponCode) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.placeOrder(couponCode));
    }

    @GetMapping
    @Operation(summary = "Get My Orders")
    public ResponseEntity<List<OrderResponse>> getMyOrders() {

        return ResponseEntity.ok(orderService.getMyOrders());
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get Order By ID")
    public ResponseEntity<OrderResponse> getOrderById(
            @PathVariable Long orderId) {

        return ResponseEntity.ok(orderService.getOrderById(orderId));
    }

    @PutMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel Order")
    public ResponseEntity<String> cancelOrder(
            @PathVariable Long orderId) {

        orderService.cancelOrder(orderId);

        return ResponseEntity.ok("Order cancelled successfully.");
    }
}
