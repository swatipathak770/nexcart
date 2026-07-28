package com.nexcart.controller;

import com.nexcart.dto.request.PaymentRequest;
import com.nexcart.dto.response.PaymentResponse;
import com.nexcart.dto.request.PaymentVerificationRequest;
import com.nexcart.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payment", description = "Payment Management APIs")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "Create Razorpay Order")
    @PostMapping("/create-order")
    public ResponseEntity<PaymentResponse> createPaymentOrder(
            @Valid @RequestBody PaymentRequest request) throws Exception {

        PaymentResponse response = paymentService.createPaymentOrder(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(summary = "Verify Razorpay Payment")
    @PostMapping("/verify")
    public ResponseEntity<PaymentResponse> verifyPayment(
            @Valid @RequestBody PaymentVerificationRequest request) throws Exception {

        PaymentResponse response = paymentService.verifyPayment(request);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get Payment By ID")
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPaymentById(
            @PathVariable Long paymentId) {

        PaymentResponse response = paymentService.getPaymentById(paymentId);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get Payment By Order ID")
    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponse> getPaymentByOrderId(
            @PathVariable Long orderId) {

        PaymentResponse response = paymentService.getPaymentByOrderId(orderId);

        return ResponseEntity.ok(response);
    }
}
