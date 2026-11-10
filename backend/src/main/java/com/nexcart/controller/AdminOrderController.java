package com.nexcart.controller;

import com.nexcart.dto.response.OrderResponse;
import com.nexcart.dto.response.PagedResponse;
import com.nexcart.entity.OrderStatus;
import com.nexcart.service.AdminOrderService;
import com.nexcart.util.PageableFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;

import java.util.Set;

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
    public ResponseEntity<PagedResponse<OrderResponse>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Pageable pageable = pageable(page, size, sortBy, direction);
        return ResponseEntity.ok(PagedResponse.from(adminOrderService.getAllOrders(pageable)));
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
    public ResponseEntity<PagedResponse<OrderResponse>> getOrdersByStatus(
            @PathVariable OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        return ResponseEntity.ok(
                PagedResponse.from(adminOrderService.getOrdersByStatus(status,
                        pageable(page, size, sortBy, direction))));
    }

    @PatchMapping("/{orderId}/status")
    @Operation(summary = "Update Order Status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam OrderStatus status) {

        return ResponseEntity.ok(
                adminOrderService.updateOrderStatus(orderId, status));
    }

    private Pageable pageable(int page, int size, String sortBy, String direction) {
        return PageableFactory.create(page, size, sortBy, direction,
                Set.of("id", "createdAt", "totalAmount", "finalAmount", "status"));
    }
}
