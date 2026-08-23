package com.nexcart.controller;

import com.nexcart.dto.response.OrderResponse;
import com.nexcart.entity.OrderStatus;
import com.nexcart.service.AdminOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@Tag(name = "Admin Order", description = "Admin Order Management APIs")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    @GetMapping
    @Operation(summary = "Get All Orders")
    public ResponseEntity<List<OrderResponse>> getAllOrders() {

        return ResponseEntity.ok(adminOrderService.getAllOrders());
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get Order By ID")
    public ResponseEntity<OrderResponse> getOrderById(
            @PathVariable Long orderId) {

        return ResponseEntity.ok(
                adminOrderService.getOrderById(orderId));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get Orders By Status")
    public ResponseEntity<List<OrderResponse>> getOrdersByStatus(
            @PathVariable OrderStatus status) {

        return ResponseEntity.ok(
                adminOrderService.getOrdersByStatus(status));
    }

    @PatchMapping("/{orderId}/status")
    @Operation(summary = "Update Order Status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam OrderStatus status) {

        return ResponseEntity.ok(
                adminOrderService.updateOrderStatus(orderId, status));
    }
}