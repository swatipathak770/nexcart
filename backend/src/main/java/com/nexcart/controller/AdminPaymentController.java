package com.nexcart.controller;

import com.nexcart.dto.response.PaymentResponse;
import com.nexcart.dto.response.PagedResponse;
import com.nexcart.entity.PaymentStatus;
import com.nexcart.service.AdminPaymentService;
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
@RequestMapping("/api/admin/payments")
@RequiredArgsConstructor
@Tag(
        name = "Admin Payment",
        description = "Admin Payment Management APIs"
)
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPaymentController {

    private final AdminPaymentService adminPaymentService;

    @GetMapping
    @Operation(summary = "Get All Payments")
    public ResponseEntity<PagedResponse<PaymentResponse>> getAllPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        return ResponseEntity.ok(
                PagedResponse.from(adminPaymentService.getAllPayments(
                        pageable(page, size, sortBy, direction))));
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "Get Payment By ID")
    public ResponseEntity<PaymentResponse> getPaymentById(
            @PathVariable Long paymentId) {

        return ResponseEntity.ok(
                adminPaymentService.getPaymentById(paymentId));
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get Payment By Order ID")
    public ResponseEntity<PaymentResponse> getPaymentByOrderId(
            @PathVariable Long orderId) {

        return ResponseEntity.ok(
                adminPaymentService.getPaymentByOrderId(orderId));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get Payments By Status")
    public ResponseEntity<PagedResponse<PaymentResponse>> getPaymentsByStatus(
            @PathVariable PaymentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        return ResponseEntity.ok(
                PagedResponse.from(adminPaymentService.getPaymentsByStatus(status,
                        pageable(page, size, sortBy, direction))));
    }

    private Pageable pageable(int page, int size, String sortBy, String direction) {
        return PageableFactory.create(page, size, sortBy, direction,
                Set.of("id", "createdAt", "amount", "paymentStatus"));
    }
}
