package org.proxiadsee.interview.task.payment.domain.dto;

public record GatewayPaymentDTO(String id, PaymentStatusDTO status, String message) {}
