package org.proxiadsee.interview.task.payment.service;

import io.grpc.stub.StreamObserver;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.proxiadsee.interview.task.payment.dto.GetPaymentRequestDTO;
import org.proxiadsee.interview.task.payment.dto.RequestPaymentRequestDTO;
import org.proxiadsee.interview.task.payment.entity.IdempotencyKeyEntity;
import org.proxiadsee.interview.task.payment.entity.PaymentEntity;
import org.proxiadsee.interview.task.payment.mapper.PaymentMapper;
import org.proxiadsee.interview.task.payment.storage.IdempotencyKeyRepository;
import org.proxiadsee.interview.task.payment.storage.PaymentRepository;
import org.proxiadsee.interview.task.payment.validation.DtoValidator;
import org.springframework.stereotype.Component;
import payments.v1.Payment.GetPaymentRequest;
import payments.v1.Payment.GetPaymentResponse;
import payments.v1.Payment.HealthRequest;
import payments.v1.Payment.HealthResponse;
import payments.v1.Payment.RequestPaymentRequest;
import payments.v1.Payment.RequestPaymentResponse;
import payments.v1.PaymentServiceGrpc.PaymentServiceImplBase;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentService extends PaymentServiceImplBase {
  private static final HealthResponse OK_HEALTH_RESPONSE =
      HealthResponse.newBuilder().setStatus("OK").build();

  private final PaymentMapper paymentMapper;
  private final DtoValidator dtoValidator;
  private final IdempotencyKeyRepository idempotencyKeyRepository;
  private final ProcessPaymentService processPaymentService;
  private final PaymentRepository paymentRepository;

  @Override
  public void requestPayment(
      RequestPaymentRequest request, StreamObserver<RequestPaymentResponse> responseObserver) {
    log.info("Request payment: {}", request);

    RequestPaymentRequestDTO dto = paymentMapper.toDto(request);
    dtoValidator.validate(dto);

    log.debug("Validated DTO: {}", dto);

    Optional<IdempotencyKeyEntity> existingEntity =
        idempotencyKeyRepository.findByValue(dto.idempotencyKey());

    RequestPaymentResponse response;
    if (existingEntity.isPresent()) {
      log.info("Idempotency key already exists: {}", dto.idempotencyKey());
      response = processPaymentService.processExistingPayment(dto, existingEntity.get());
    } else {
      log.info("Creating new idempotency key: {}", dto.idempotencyKey());
      IdempotencyKeyEntity newEntity = new IdempotencyKeyEntity();
      newEntity.setValue(dto.idempotencyKey());
      newEntity.setRequestHash(String.valueOf(dto.hashCode()));
      newEntity.setCreatedAt(LocalDateTime.now());
      response = processPaymentService.processNewPayment(dto, newEntity);
    }

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void getPayment(
      GetPaymentRequest request, StreamObserver<GetPaymentResponse> responseObserver) {
    log.info("Get payment: {}", request);

    GetPaymentRequestDTO dto = paymentMapper.toDto(request);
    dtoValidator.validate(dto);

    log.debug("Validated DTO: {}", dto);

    Long id = Long.parseLong(dto.paymentId());
    Optional<PaymentEntity> entityOpt = paymentRepository.findById(id);

    if (entityOpt.isEmpty()) {
      responseObserver.onNext(GetPaymentResponse.newBuilder().build());
    } else {
      GetPaymentResponse response = paymentMapper.toGetPaymentResponse(entityOpt.get());
      responseObserver.onNext(response);
    }
    responseObserver.onCompleted();
  }

  @Override
  public void health(HealthRequest request, StreamObserver<HealthResponse> responseObserver) {
    log.debug("Health check");
    responseObserver.onNext(OK_HEALTH_RESPONSE);
    responseObserver.onCompleted();
  }
}
