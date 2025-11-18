package org.proxiadsee.interview.task.payment.service;

import io.grpc.Status;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.proxiadsee.interview.task.payment.domain.dto.GatewayPaymentDTO;
import org.proxiadsee.interview.task.payment.domain.dto.RequestPaymentRequestDTO;
import org.proxiadsee.interview.task.payment.domain.entity.IdempotencyKeyEntity;
import org.proxiadsee.interview.task.payment.domain.entity.PaymentEntity;
import org.proxiadsee.interview.task.payment.mapper.PaymentMapper;
import org.proxiadsee.interview.task.payment.storage.IdempotencyKeyRepository;
import org.proxiadsee.interview.task.payment.storage.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import payments.v1.Payment.RequestPaymentResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessPaymentService {

  private final PaymentRepository paymentRepository;
  private final IdempotencyKeyRepository idempotencyKeyRepository;
  private final PaymentMapper paymentMapper;
  private final PaymentGatewayService paymentGatewayService;

  @Transactional(rollbackFor = Exception.class)
  public RequestPaymentResponse processNewPayment(
      RequestPaymentRequestDTO dto, IdempotencyKeyEntity idempotencyKeyEntity) {
    log.info("Processing new payment for idempotency key: {}", dto.idempotencyKey());

    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCompletion(int status) {
            if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
              try {
                idempotencyKeyRepository.delete(idempotencyKeyEntity);
              } catch (Exception ex) {
                log.warn(
                    "Failed to delete idempotency key after rollback: {}",
                    idempotencyKeyEntity.getId(),
                    ex);
              }
            }
          }
        });

    try {
      PaymentEntity paymentEntity = paymentMapper.toPaymentEntity(dto, idempotencyKeyEntity);
      PaymentEntity savedEntity = paymentRepository.save(paymentEntity);

      GatewayPaymentDTO gatewayResponse = paymentGatewayService.processPayment(dto);

      savedEntity.setGatewayPaymentId(gatewayResponse.id());
      savedEntity.setStatus(gatewayResponse.status().name());
      savedEntity.setMessage(gatewayResponse.message());

      PaymentEntity updatedEntity = paymentRepository.save(savedEntity);

      return paymentMapper.toRequestPaymentResponse(updatedEntity);
    } catch (Exception e) {
      log.error("Error processing new payment for idempotency key: {}", dto.idempotencyKey(), e);
      throw Status.INTERNAL
          .withDescription("Failed to process payment")
          .withCause(e)
          .asRuntimeException();
    }
  }

  public RequestPaymentResponse processExistingPayment(
      RequestPaymentRequestDTO dto, IdempotencyKeyEntity idempotencyKeyEntity) {
    log.info(
        "Processing existing payment for idempotency key: {}", idempotencyKeyEntity.getValue());

    Optional<PaymentEntity> paymentOpt =
        paymentRepository.findByIdempotencyKey_Id(idempotencyKeyEntity.getId());

    if (paymentOpt.isPresent()) {
      return paymentMapper.toRequestPaymentResponse(paymentOpt.get());
    } else {
      return paymentMapper.toRequestPaymentResponse(dto, idempotencyKeyEntity);
    }
  }
}
