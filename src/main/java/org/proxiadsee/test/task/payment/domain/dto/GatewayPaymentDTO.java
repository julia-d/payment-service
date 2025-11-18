package domain.dto;

public record GatewayPaymentDTO(
    String id,
    PaymentStatusDTO status,
    String message) {
}

