package com.nexcart.controller;

import com.nexcart.dto.response.PaymentResponse;
import com.nexcart.entity.PaymentStatus;
import com.nexcart.service.AdminPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<List<PaymentResponse>> getAllPayments() {

        return ResponseEntity.ok(
                adminPaymentService.getAllPayments());
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
    public ResponseEntity<List<PaymentResponse>> getPaymentsByStatus(
            @PathVariable PaymentStatus status) {

        return ResponseEntity.ok(
                adminPaymentService.getPaymentsByStatus(status));
    }
}
