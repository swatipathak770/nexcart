package com.nexcart.dto.response;



import com.nexcart.entity.PaymentMethod;
import com.nexcart.entity.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponse {

    private Long paymentId;

    private Long orderId;

    private BigDecimal amount;

    private PaymentStatus paymentStatus;

    private PaymentMethod paymentMethod;

    private String transactionId;

    private String razorpayOrderId;

    private String razorpayPaymentId;

    private String failureReason;

    private LocalDateTime paidAt;

    private LocalDateTime createdAt;
}
