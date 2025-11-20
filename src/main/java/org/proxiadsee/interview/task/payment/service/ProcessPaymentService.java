package org.proxiadsee.interview.task.payment.service;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.proxiadsee.interview.task.payment.domain.dto.GatewayPaymentDTO;
import org.proxiadsee.interview.task.payment.domain.dto.RequestPaymentRequestDTO;
import org.proxiadsee.interview.task.payment.domain.entity.IdempotencyKeyEntity;
import org.proxiadsee.interview.task.payment.domain.entity.PaymentEntity;
import org.proxiadsee.interview.task.payment.exception.ConflictException;
import org.proxiadsee.interview.task.payment.mapper.PaymentMapper;
import org.proxiadsee.interview.task.payment.storage.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import payments.v1.Payment.RequestPaymentResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessPaymentService {

  private final PaymentRepository paymentRepository;
  private final PaymentMapper paymentMapper;
  private final PaymentGatewayService paymentGatewayService;

  @Transactional(rollbackFor = Exception.class)
  public RequestPaymentResponse processNewPayment(
      RequestPaymentRequestDTO dto, IdempotencyKeyEntity idempotencyKeyEntity) {
    log.info("Processing new payment, orderId={}", dto.orderId());

    PaymentEntity paymentEntity = paymentMapper.toPaymentEntity(dto, idempotencyKeyEntity);
    PaymentEntity savedEntity = paymentRepository.save(paymentEntity);

    log.debug(
        "Payment entity created, paymentId={}, orderId={}", savedEntity.getId(), dto.orderId());

    GatewayPaymentDTO gatewayResponse = paymentGatewayService.processPayment(dto);

    PaymentEntity updatedEntity = savedEntity;
    try {
      savedEntity.setGatewayPaymentId(gatewayResponse.id());
      savedEntity.setStatus(gatewayResponse.status().name());
      savedEntity.setMessage(gatewayResponse.message());
      updatedEntity = paymentRepository.save(savedEntity);
      log.info(
          "Payment processed successfully, paymentId={}, orderId={}, status={}, gatewayId={}",
          updatedEntity.getId(),
          dto.orderId(),
          gatewayResponse.status().name(),
          gatewayResponse.id());
    } catch (Exception e) {
      log.warn(
          "Failed to update payment entity after gateway response, paymentId={}, orderId={}, scheduling retry after transaction commit",
          savedEntity.getId(),
          dto.orderId(),
          e);
    }

    return paymentMapper.toRequestPaymentResponse(updatedEntity);
  }

  public RequestPaymentResponse processExistingPayment(
      RequestPaymentRequestDTO dto, IdempotencyKeyEntity idempotencyKeyEntity) {
    log.info(
        "Processing existing payment, orderId={}, idempotencyKeyId={}",
        dto.orderId(),
        idempotencyKeyEntity.getId());

    validateRequestHash(dto, idempotencyKeyEntity);

    Optional<PaymentEntity> paymentOpt =
        paymentRepository.findByIdempotencyKeyId(idempotencyKeyEntity.getId());

    if (paymentOpt.isPresent()) {
      PaymentEntity entity = paymentOpt.get();
      log.info(
          "Found existing payment: paymentId={}, orderId={}, status={}, gatewayId={}",
          entity.getId(),
          entity.getOrderId(),
          entity.getStatus(),
          entity.getGatewayPaymentId());
      return paymentMapper.toRequestPaymentResponse(entity);
    } else {
      log.warn(
          "No payment found for idempotency key ID: {}, orderId={}, returning default response",
          idempotencyKeyEntity.getId(),
          dto.orderId());
      return paymentMapper.toRequestPaymentResponse(dto, idempotencyKeyEntity);
    }
  }

  private void validateRequestHash(
      RequestPaymentRequestDTO dto, IdempotencyKeyEntity idempotencyKeyEntity) {
    String currentHash = String.valueOf(dto.hashCode());
    String storedHash = idempotencyKeyEntity.getRequestHash();

    if (!currentHash.equals(storedHash)) {
      log.error(
          "Hash mismatch for payment request, idempotencyKeyId={}, orderId={}, currentHash={}, storedHash={}",
          idempotencyKeyEntity.getId(),
          dto.orderId(),
          currentHash,
          storedHash);
      throw new ConflictException(
          "Request hash does not match stored hash for the provided idempotency key");
    }
  }
}
