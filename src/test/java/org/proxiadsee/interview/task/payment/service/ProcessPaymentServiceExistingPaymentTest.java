package org.proxiadsee.interview.task.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.proxiadsee.interview.task.payment.domain.dto.GatewayPaymentDTO;
import org.proxiadsee.interview.task.payment.domain.dto.PaymentStatusDTO;
import org.proxiadsee.interview.task.payment.domain.dto.RequestPaymentRequestDTO;
import org.proxiadsee.interview.task.payment.domain.entity.IdempotencyKeyEntity;
import org.proxiadsee.interview.task.payment.domain.entity.PaymentEntity;
import org.proxiadsee.interview.task.payment.mapper.PaymentMapper;
import org.proxiadsee.interview.task.payment.storage.IdempotencyKeyRepository;
import org.proxiadsee.interview.task.payment.storage.PaymentRepository;
import payments.v1.Payment.RequestPaymentResponse;

@DisplayName("ProcessPaymentService Tests")
@ExtendWith(MockitoExtension.class)
class ProcessPaymentServiceExistingPaymentTest {

  @Mock private PaymentRepository paymentRepository;

  @Mock private IdempotencyKeyRepository idempotencyKeyRepository;

  @Mock private PaymentMapper paymentMapper;

  @Mock private PaymentGatewayService paymentGatewayService;

  private ProcessPaymentService processPaymentService;

  @BeforeEach
  void setUp() {
    processPaymentService =
        new ProcessPaymentService(
            paymentRepository, paymentMapper, paymentGatewayService);
  }

  @DisplayName("processExistingPayment - Happy path: payment exists")
  @Test
  void testProcessExistingPaymentWithExistingPayment() {
    RequestPaymentRequestDTO dto = createValidRequestPaymentDTO();
    IdempotencyKeyEntity idempotencyKeyEntity = createValidIdempotencyKeyEntity();
    PaymentEntity existingPayment = createValidPaymentEntity();
    existingPayment.setId(1L);
    RequestPaymentResponse expectedResponse = createValidRequestPaymentResponse();

    when(paymentRepository.findByIdempotencyKey_Id(idempotencyKeyEntity.getId()))
        .thenReturn(Optional.of(existingPayment));
    when(paymentMapper.toRequestPaymentResponse(existingPayment)).thenReturn(expectedResponse);

    RequestPaymentResponse response =
        processPaymentService.processExistingPayment(dto, idempotencyKeyEntity);

    assertNotNull(response);
    assertEquals(expectedResponse, response);
    verify(paymentRepository).findByIdempotencyKey_Id(idempotencyKeyEntity.getId());
    verify(paymentMapper).toRequestPaymentResponse(existingPayment);
    verify(paymentMapper, never()).toRequestPaymentResponse(dto, idempotencyKeyEntity);
  }

  @DisplayName(
      "processExistingPayment - Happy path: payment does not exist, creates response from DTO")
  @Test
  void testProcessExistingPaymentWithoutExistingPayment() {
    RequestPaymentRequestDTO dto = createValidRequestPaymentDTO();
    IdempotencyKeyEntity idempotencyKeyEntity = createValidIdempotencyKeyEntity();
    RequestPaymentResponse expectedResponse = createValidRequestPaymentResponse();

    when(paymentRepository.findByIdempotencyKey_Id(idempotencyKeyEntity.getId()))
        .thenReturn(Optional.empty());
    when(paymentMapper.toRequestPaymentResponse(dto, idempotencyKeyEntity))
        .thenReturn(expectedResponse);

    RequestPaymentResponse response =
        processPaymentService.processExistingPayment(dto, idempotencyKeyEntity);

    assertNotNull(response);
    assertEquals(expectedResponse, response);
    verify(paymentRepository).findByIdempotencyKey_Id(idempotencyKeyEntity.getId());
    verify(paymentMapper).toRequestPaymentResponse(dto, idempotencyKeyEntity);
    verify(paymentMapper, never()).toRequestPaymentResponse(any(PaymentEntity.class));
  }

  @DisplayName("processExistingPayment - Repository throws exception")
  @Test
  void testProcessExistingPaymentRepositoryException() {
    RequestPaymentRequestDTO dto = createValidRequestPaymentDTO();
    IdempotencyKeyEntity idempotencyKeyEntity = createValidIdempotencyKeyEntity();

    when(paymentRepository.findByIdempotencyKey_Id(idempotencyKeyEntity.getId()))
        .thenThrow(new RuntimeException("Database error"));

    RuntimeException exception =
        assertThrows(
            RuntimeException.class,
            () -> processPaymentService.processExistingPayment(dto, idempotencyKeyEntity));

    assertEquals("Database error", exception.getMessage());
    verify(paymentRepository).findByIdempotencyKey_Id(idempotencyKeyEntity.getId());
    verify(paymentMapper, never()).toRequestPaymentResponse(any(PaymentEntity.class));
  }

  @DisplayName("processExistingPayment - Mapper throws exception for existing payment")
  @Test
  void testProcessExistingPaymentMapperExceptionWithPayment() {
    RequestPaymentRequestDTO dto = createValidRequestPaymentDTO();
    IdempotencyKeyEntity idempotencyKeyEntity = createValidIdempotencyKeyEntity();
    PaymentEntity existingPayment = createValidPaymentEntity();

    when(paymentRepository.findByIdempotencyKey_Id(idempotencyKeyEntity.getId()))
        .thenReturn(Optional.of(existingPayment));
    when(paymentMapper.toRequestPaymentResponse(existingPayment))
        .thenThrow(new RuntimeException("Mapping error"));

    RuntimeException exception =
        assertThrows(
            RuntimeException.class,
            () -> processPaymentService.processExistingPayment(dto, idempotencyKeyEntity));

    assertEquals("Mapping error", exception.getMessage());
    verify(paymentMapper).toRequestPaymentResponse(existingPayment);
  }

  @DisplayName("processExistingPayment - Mapper throws exception for DTO mapping")
  @Test
  void testProcessExistingPaymentMapperExceptionWithDTO() {
    RequestPaymentRequestDTO dto = createValidRequestPaymentDTO();
    IdempotencyKeyEntity idempotencyKeyEntity = createValidIdempotencyKeyEntity();

    when(paymentRepository.findByIdempotencyKey_Id(idempotencyKeyEntity.getId()))
        .thenReturn(Optional.empty());
    when(paymentMapper.toRequestPaymentResponse(dto, idempotencyKeyEntity))
        .thenThrow(new RuntimeException("DTO mapping error"));

    RuntimeException exception =
        assertThrows(
            RuntimeException.class,
            () -> processPaymentService.processExistingPayment(dto, idempotencyKeyEntity));

    assertEquals("DTO mapping error", exception.getMessage());
    verify(paymentMapper).toRequestPaymentResponse(dto, idempotencyKeyEntity);
  }

  @DisplayName("processExistingPayment - Null idempotency key entity handling")
  @Test
  void testProcessExistingPaymentNullIdempotencyKey() {
    RequestPaymentRequestDTO dto = createValidRequestPaymentDTO();

    assertThrows(
        NullPointerException.class, () -> processPaymentService.processExistingPayment(dto, null));
  }

  private RequestPaymentRequestDTO createValidRequestPaymentDTO() {
    return new RequestPaymentRequestDTO(
        1000L, "USD", "ORDER123", "IDEMPOTENT-KEY-123", new HashMap<>());
  }

  private IdempotencyKeyEntity createValidIdempotencyKeyEntity() {
    IdempotencyKeyEntity entity = new IdempotencyKeyEntity();
    entity.setId(1L);
    entity.setValue("IDEMPOTENT-KEY-123");
    entity.setRequestHash("somehash");
    entity.setCreatedAt(LocalDateTime.now());
    return entity;
  }

  private PaymentEntity createValidPaymentEntity() {
    PaymentEntity entity = new PaymentEntity();
    entity.setAmountMinor(1000L);
    entity.setCurrency("USD");
    entity.setOrderId("ORDER123");
    entity.setStatus("PENDING");
    entity.setCreatedAt(LocalDateTime.now());
    return entity;
  }

  private RequestPaymentResponse createValidRequestPaymentResponse() {
    return RequestPaymentResponse.newBuilder().setPaymentId("1").build();
  }
}
