package org.proxiadsee.test.task.payment.service;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.proxiadsee.test.task.payment.dto.RequestPaymentRequestDTO;
import org.proxiadsee.test.task.payment.entity.IdempotencyKeyEntity;
import org.proxiadsee.test.task.payment.entity.PaymentEntity;
import org.proxiadsee.test.task.payment.mapper.PaymentMapper;
import org.proxiadsee.test.task.payment.storage.PaymentRepository;
import org.springframework.stereotype.Service;
import payments.v1.Payment.RequestPaymentResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessPaymentService {

  private final PaymentRepository paymentRepository;
  private final PaymentMapper paymentMapper;

  public RequestPaymentResponse processNewPayment(
      RequestPaymentRequestDTO dto, IdempotencyKeyEntity idempotencyKeyEntity) {
    log.info("Processing new payment for idempotency key: {}", dto.idempotencyKey());
    return RequestPaymentResponse.newBuilder()
        .setPaymentId("")
        .setIdempotencyKey(dto.idempotencyKey())
        .build();
  }

  public RequestPaymentResponse processExistingPayment(
      RequestPaymentRequestDTO dto, IdempotencyKeyEntity idempotencyKeyEntity) {
    log.info(
        "Processing existing payment for idempotency key: {}", idempotencyKeyEntity.getValue());

    Optional<PaymentEntity> paymentOpt =
        paymentRepository.findByIdempotencyKey_Id(idempotencyKeyEntity.getId());

    if (paymentOpt.isPresent()) {
      return paymentMapper.toRequestPaymentResponse(paymentOpt.get());
    }

    return RequestPaymentResponse.newBuilder()
        .setPaymentId("")
        .setIdempotencyKey(idempotencyKeyEntity.getValue())
        .build();
  }
}
