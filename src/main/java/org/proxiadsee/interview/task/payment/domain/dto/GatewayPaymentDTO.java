package org.proxiadsee.interview.task.payment.dto;

public record GatewayPaymentDTO(String id, PaymentStatusDTO status, String message) {}
