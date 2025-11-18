package org.proxiadsee.interview.task.payment.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record GetPaymentRequestDTO(
    @NotBlank(message = "Payment ID is required")
        @Pattern(regexp = "^\\d+$", message = "Payment ID must be numeric")
        String paymentId) {}
