package com.nexcart.controller;

import com.nexcart.dto.response.OrderResponse;
import com.nexcart.dto.response.PagedResponse;
import com.nexcart.dto.request.PlaceOrderRequest;
import com.nexcart.service.OrderService;
import com.nexcart.util.PageableFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;

import java.util.Set;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@RestController
@Validated
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Order", description = "Order Management APIs")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Place an Order")
    public ResponseEntity<OrderResponse> placeOrder(
            @RequestParam(required = false) String couponCode,
            @RequestHeader("Idempotency-Key")
            @NotBlank @Size(max = 100) @Pattern(regexp = "[A-Za-z0-9._:-]{16,100}") String idempotencyKey,
            @Valid @RequestBody PlaceOrderRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.placeOrder(couponCode, idempotencyKey, request.getAddressId()));
    }

    @GetMapping
    @Operation(summary = "Get My Orders")
    public ResponseEntity<PagedResponse<OrderResponse>> getMyOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Pageable pageable = PageableFactory.create(page, size, sortBy, direction,
                Set.of("id", "createdAt", "totalAmount", "finalAmount", "status"));
        return ResponseEntity.ok(PagedResponse.from(orderService.getMyOrders(pageable)));
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
