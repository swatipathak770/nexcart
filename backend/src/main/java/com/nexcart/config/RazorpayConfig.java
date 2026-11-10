package com.nexcart.config;

import com.razorpay.RazorpayClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RazorpayConfig {

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    @Value("${razorpay.mode:TEST}")
    private String mode;

    @Bean
    public RazorpayClient razorpayClient() throws Exception {
        if (keyId == null || keyId.isBlank() || keySecret == null || keySecret.isBlank()) {
            throw new IllegalStateException("Razorpay credentials are not configured.");
        }
        String normalizedMode = mode.trim().toUpperCase(java.util.Locale.ROOT);
        if (!normalizedMode.equals("TEST") && !normalizedMode.equals("LIVE")) {
            throw new IllegalStateException("RAZORPAY_MODE must be TEST or LIVE.");
        }
        String requiredPrefix = normalizedMode.equals("TEST") ? "rzp_test_" : "rzp_live_";
        if (!keyId.trim().startsWith(requiredPrefix)) {
            throw new IllegalStateException("Razorpay key does not match the explicitly configured " + normalizedMode + " mode.");
        }
        return new RazorpayClient(keyId.trim(), keySecret.trim());
    }
}
