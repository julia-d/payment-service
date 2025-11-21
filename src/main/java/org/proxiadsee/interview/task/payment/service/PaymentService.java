package org.proxiadsee.interview.task.payment.service;

import io.grpc.stub.StreamObserver;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.proxiadsee.interview.task.payment.domain.dto.GetPaymentRequestDTO;
import org.proxiadsee.interview.task.payment.domain.dto.RequestPaymentRequestDTO;
import org.proxiadsee.interview.task.payment.domain.entity.IdempotencyKeyEntity;
import org.proxiadsee.interview.task.payment.domain.entity.PaymentEntity;
import org.proxiadsee.interview.task.payment.exception.ConflictException;
import org.proxiadsee.interview.task.payment.exception.ServiceException;
import org.proxiadsee.interview.task.payment.mapper.PaymentMapper;
import org.proxiadsee.interview.task.payment.storage.IdempotencyKeyRepository;
import org.proxiadsee.interview.task.payment.storage.PaymentRepository;
import org.proxiadsee.interview.task.payment.validation.DtoValidator;
import org.springframework.grpc.server.service.GrpcService;
import payments.v1.Payment.GetPaymentRequest;
import payments.v1.Payment.GetPaymentResponse;
import payments.v1.Payment.HealthRequest;
import payments.v1.Payment.HealthResponse;
import payments.v1.Payment.RequestPaymentRequest;
import payments.v1.Payment.RequestPaymentResponse;
import payments.v1.PaymentServiceGrpc.PaymentServiceImplBase;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class PaymentService extends PaymentServiceImplBase {
  private static final HealthResponse OK_HEALTH_RESPONSE =
      HealthResponse.newBuilder().setStatus("OK").build();

  private final PaymentMapper paymentMapper;
  private final DtoValidator dtoValidator;
  private final IdempotencyKeyRepository idempotencyKeyRepository;
  private final ProcessPaymentService processPaymentService;
  private final PaymentRepository paymentRepository;
  private final IdempotencyKeyHashingService hashingService;

  @Override
  public void requestPayment(
      RequestPaymentRequest request, StreamObserver<RequestPaymentResponse> responseObserver) {
    RequestPaymentRequestDTO dto = paymentMapper.toDto(request);
    log.info("Request payment received, orderId={}", dto.orderId());

    dtoValidator.validate(dto);

    log.debug("DTO validated successfully, orderId={}", dto.orderId());

    RequestPaymentResponse response;

    try {
      String hashedKey = hashingService.hash(dto.idempotencyKey());
      Optional<IdempotencyKeyEntity> existingEntity =
          idempotencyKeyRepository.findByValue(hashedKey);

      if (existingEntity.isPresent()) {
        log.info(
            "Idempotency key already exists, processing existing payment, orderId={}",
            dto.orderId());
        response = processPaymentService.processExistingPayment(dto, existingEntity.get());
      } else {
        response = processNewPayment(dto);
      }
    } catch (ConflictException | ServiceException e) {
      throw e;
    } catch (Exception e) {
      log.error("Error processing payment request, orderId={}", dto.orderId(), e);
      throw new ServiceException("Failed to process payment", e);
    }

    log.info(
        "Payment request completed successfully, orderId={}, paymentId={}",
        dto.orderId(),
        response.getPaymentId());
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  private RequestPaymentResponse processNewPayment(RequestPaymentRequestDTO dto) {
    RequestPaymentResponse response;
    log.info("Creating new idempotency key, orderId={}", dto.orderId());
    IdempotencyKeyEntity newEntity = paymentMapper.toIdempotencyKey(dto);
    idempotencyKeyRepository.save(newEntity);
    try {
      response = processPaymentService.processNewPayment(dto, newEntity);
    } catch (Exception e) {
      log.error(
          "Error processing new payment, deleting idempotency key, orderId={}", dto.orderId(), e);
      idempotencyKeyRepository.delete(newEntity);
      throw new ServiceException("Failed to process new payment", e);
    }
    return response;
  }

  @Override
  public void getPayment(
      GetPaymentRequest request, StreamObserver<GetPaymentResponse> responseObserver) {
    GetPaymentRequestDTO dto = paymentMapper.toDto(request);
    log.info("Get payment request received, paymentId={}", dto.paymentId());

    dtoValidator.validate(dto);

    log.debug("DTO validated successfully, paymentId={}", dto.paymentId());

    Long id = Long.parseLong(dto.paymentId());
    Optional<PaymentEntity> entityOpt = paymentRepository.findById(id);

    if (entityOpt.isEmpty()) {
      log.info("Payment not found, paymentId={}", dto.paymentId());
      responseObserver.onNext(GetPaymentResponse.newBuilder().build());
    } else {
      PaymentEntity entity = entityOpt.get();
      log.info(
          "Payment found, paymentId={}, orderId={}, status={}",
          dto.paymentId(),
          entity.getOrderId(),
          entity.getStatus());
      GetPaymentResponse response = paymentMapper.toGetPaymentResponse(entity);
      responseObserver.onNext(response);
    }
    responseObserver.onCompleted();
  }

  @Override
  public void health(HealthRequest request, StreamObserver<HealthResponse> responseObserver) {
    log.info("Health check");
    responseObserver.onNext(OK_HEALTH_RESPONSE);
    responseObserver.onCompleted();
  }
}
