package org.proxiadsee.interview.task.payment.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.util.Map;

public record RequestPaymentRequestDTO(
    @Positive(message = "Amount must be positive") long amountMinor,
    @NotBlank(message = "Currency is required")
        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid ISO 4217 code")
        String currency,
    @NotBlank(message = "Order ID is required") String orderId,
    @NotBlank(message = "Idempotency key is required") String idempotencyKey,
    Map<String, String> metadata) {}
