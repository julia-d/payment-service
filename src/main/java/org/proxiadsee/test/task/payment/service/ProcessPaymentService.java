package org.proxiadsee.test.task.payment.service;

import io.grpc.Status;
import lombok.extern.slf4j.Slf4j;
import org.proxiadsee.test.task.payment.dto.RequestPaymentRequestDTO;
import org.proxiadsee.test.task.payment.entity.IdempotencyKeyEntity;
import org.springframework.stereotype.Service;
import payments.v1.Payment.RequestPaymentResponse;

@Slf4j
@Service
public class ProcessPaymentService {

  public RequestPaymentResponse processNewPayment(
      RequestPaymentRequestDTO dto, IdempotencyKeyEntity idempotencyKeyEntity) {
    log.info("Processing new payment for idempotency key: {}", dto.idempotencyKey());

    return RequestPaymentResponse.newBuilder().build();
  }

  public RequestPaymentResponse processExistingPayment(
      RequestPaymentRequestDTO dto, IdempotencyKeyEntity idempotencyKeyEntity) {
    log.info(
        "Processing existing payment for idempotency key: {}", idempotencyKeyEntity.getValue());

    String dtoHash = String.valueOf(dto.hashCode());
    String entityHash = idempotencyKeyEntity.getRequestHash();

    if (!entityHash.equals(dtoHash)) {
      log.warn(
          "Hash mismatch for idempotency key: {}. Expected: {}, Received: {}",
          idempotencyKeyEntity.getValue(),
          entityHash,
          dtoHash);
      throw Status.ALREADY_EXISTS
          .withDescription(
              "Idempotency key already exists with different request parameters. Conflict detected.")
          .asRuntimeException();
    }

    log.info("Hash match confirmed for idempotency key: {}", idempotencyKeyEntity.getValue());
    return RequestPaymentResponse.newBuilder().build();
  }
}
