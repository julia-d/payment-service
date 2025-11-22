package org.proxiadsee.interview.task.payment.domain.dto;

import payments.v1.Payment.PaymentStatus;

public enum PaymentStatusDTO {
  PAYMENT_STATUS_UNSPECIFIED(PaymentStatus.PAYMENT_STATUS_UNSPECIFIED),
  PAYMENT_STATUS_PENDING(PaymentStatus.PAYMENT_STATUS_PENDING),
  PAYMENT_STATUS_SUCCEEDED(PaymentStatus.PAYMENT_STATUS_SUCCEEDED),
  PAYMENT_STATUS_FAILED(PaymentStatus.PAYMENT_STATUS_FAILED);

  private final PaymentStatus protoStatus;

  PaymentStatusDTO(PaymentStatus protoStatus) {
    this.protoStatus = protoStatus;
  }

  public PaymentStatus toProtoStatus() {
    return protoStatus;
  }

  public static PaymentStatusDTO fromProtoStatus(PaymentStatus protoStatus) {
    for (PaymentStatusDTO status : PaymentStatusDTO.values()) {
      if (status.protoStatus == protoStatus) {
        return status;
      }
    }
    return PAYMENT_STATUS_UNSPECIFIED;
  }
}
