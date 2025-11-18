package org.proxiadsee.interview.task.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.HashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.proxiadsee.interview.task.payment.domain.dto.GatewayPaymentDTO;
import org.proxiadsee.interview.task.payment.domain.dto.PaymentStatusDTO;
import org.proxiadsee.interview.task.payment.domain.dto.RequestPaymentRequestDTO;
import org.proxiadsee.interview.task.payment.domain.entity.IdempotencyKeyEntity;
import org.proxiadsee.interview.task.payment.domain.entity.PaymentEntity;
import org.proxiadsee.interview.task.payment.mapper.PaymentMapper;
import org.proxiadsee.interview.task.payment.storage.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import payments.v1.Payment.RequestPaymentResponse;

@DisplayName("ProcessPaymentService Spring Boot Unit Tests")
@SpringBootTest(classes = {ProcessPaymentService.class})
class ProcessPaymentServiceNewPaymentTest {

  @Autowired private ProcessPaymentService processPaymentService;

  @MockitoBean private PaymentRepository paymentRepository;

  @MockitoBean private PaymentMapper paymentMapper;

  @MockitoBean
  private PaymentGatewayService paymentGatewayService;

  private RequestPaymentRequestDTO validDTO;
  private IdempotencyKeyEntity idempotencyKeyEntity;

  @BeforeEach
  void setUp() {
    validDTO =
        new RequestPaymentRequestDTO(
            5000L, "EUR", "ORDER-INT-001", "IDEMPOTENCY-INT-001", new HashMap<>());

    idempotencyKeyEntity = new IdempotencyKeyEntity();
    idempotencyKeyEntity.setValue("IDEMPOTENCY-INT-001");
    idempotencyKeyEntity.setRequestHash(String.valueOf(validDTO.hashCode()));
    idempotencyKeyEntity.setCreatedAt(LocalDateTime.now());
  }

  @DisplayName("processNewPayment - Successfully processes payment with mocked dependencies")
  @Test
  void testProcessNewPaymentSuccess() {
    PaymentEntity initialPayment = createValidPaymentEntity();
    PaymentEntity savedPayment = createValidPaymentEntity();
    savedPayment.setId(1L);

    PaymentEntity updatedPayment = createValidPaymentEntity();
    updatedPayment.setId(1L);
    updatedPayment.setGatewayPaymentId("GATEWAY-123");
    updatedPayment.setStatus("PENDING");
    updatedPayment.setMessage("Payment processed");

    GatewayPaymentDTO gatewayResponse = new GatewayPaymentDTO("GATEWAY-123", PaymentStatusDTO.PAYMENT_STATUS_PENDING, "Payment processed");
    RequestPaymentResponse expectedResponse = RequestPaymentResponse.newBuilder().setPaymentId("1").build();

    when(paymentMapper.toPaymentEntity(validDTO, idempotencyKeyEntity)).thenReturn(initialPayment);
    when(paymentRepository.save(initialPayment)).thenReturn(savedPayment);
    when(paymentGatewayService.processPayment(validDTO)).thenReturn(gatewayResponse);
    when(paymentRepository.save(any(PaymentEntity.class))).thenReturn(updatedPayment);
    when(paymentMapper.toRequestPaymentResponse(updatedPayment)).thenReturn(expectedResponse);

    RequestPaymentResponse response = processPaymentService.processNewPayment(validDTO, idempotencyKeyEntity);

    assertNotNull(response);
    assertEquals("1", response.getPaymentId());
    verify(paymentRepository, times(2)).save(any(PaymentEntity.class));
    verify(paymentGatewayService).processPayment(validDTO);
    verify(paymentMapper).toPaymentEntity(validDTO, idempotencyKeyEntity);
    verify(paymentMapper).toRequestPaymentResponse(updatedPayment);
  }

  @DisplayName("processNewPayment - Gateway service exception handling")
  @Test
  void testProcessNewPaymentGatewayException() {
    PaymentEntity initialPayment = createValidPaymentEntity();
    PaymentEntity savedPayment = createValidPaymentEntity();
    savedPayment.setId(1L);

    when(paymentMapper.toPaymentEntity(validDTO, idempotencyKeyEntity)).thenReturn(initialPayment);
    when(paymentRepository.save(initialPayment)).thenReturn(savedPayment);
    when(paymentGatewayService.processPayment(validDTO)).thenThrow(new RuntimeException("Gateway error"));

    assertThrows(RuntimeException.class, () ->
        processPaymentService.processNewPayment(validDTO, idempotencyKeyEntity));

    verify(paymentRepository, times(1)).save(initialPayment);
    verify(paymentGatewayService).processPayment(validDTO);
  }

  @DisplayName("processNewPayment - Mapper exception on toPaymentEntity")
  @Test
  void testProcessNewPaymentMapperToEntityException() {
    when(paymentMapper.toPaymentEntity(validDTO, idempotencyKeyEntity))
        .thenThrow(new RuntimeException("Mapping to entity failed"));

    assertThrows(RuntimeException.class, () ->
        processPaymentService.processNewPayment(validDTO, idempotencyKeyEntity));

    verify(paymentMapper).toPaymentEntity(validDTO, idempotencyKeyEntity);
    verify(paymentRepository, times(0)).save(any(PaymentEntity.class));
    verify(paymentGatewayService, times(0)).processPayment(any());
  }

  @DisplayName("processNewPayment - Repository exception on first save")
  @Test
  void testProcessNewPaymentFirstSaveException() {
    PaymentEntity initialPayment = createValidPaymentEntity();

    when(paymentMapper.toPaymentEntity(validDTO, idempotencyKeyEntity)).thenReturn(initialPayment);
    when(paymentRepository.save(initialPayment)).thenThrow(new RuntimeException("Database error on first save"));

    assertThrows(RuntimeException.class, () ->
        processPaymentService.processNewPayment(validDTO, idempotencyKeyEntity));

    verify(paymentMapper).toPaymentEntity(validDTO, idempotencyKeyEntity);
    verify(paymentRepository, times(1)).save(initialPayment);
    verify(paymentGatewayService, times(0)).processPayment(any());
  }

  @DisplayName("processNewPayment - Repository exception on second save")
  @Test
  void testProcessNewPaymentSecondSaveException() {
    PaymentEntity initialPayment = createValidPaymentEntity();
    PaymentEntity savedPayment = createValidPaymentEntity();
    savedPayment.setId(1L);
    GatewayPaymentDTO gatewayResponse =
        new GatewayPaymentDTO(
            "GATEWAY-123", PaymentStatusDTO.PAYMENT_STATUS_SUCCEEDED, "Payment processed");
    RequestPaymentResponse expectedResponse =
        RequestPaymentResponse.newBuilder().setPaymentId("1").build();

    when(paymentMapper.toPaymentEntity(validDTO, idempotencyKeyEntity)).thenReturn(initialPayment);
    when(paymentRepository.save(any(PaymentEntity.class)))
        .thenReturn(savedPayment)
        .thenThrow(new RuntimeException("Database error on second save"));
    when(paymentGatewayService.processPayment(validDTO)).thenReturn(gatewayResponse);
    when(paymentMapper.toRequestPaymentResponse(savedPayment)).thenReturn(expectedResponse);

    RequestPaymentResponse response =
        processPaymentService.processNewPayment(validDTO, idempotencyKeyEntity);

    assertNotNull(response);
    assertEquals(expectedResponse, response);
    verify(paymentMapper).toPaymentEntity(validDTO, idempotencyKeyEntity);
    verify(paymentRepository, times(2)).save(any(PaymentEntity.class));
    verify(paymentGatewayService).processPayment(validDTO);
    verify(paymentMapper).toRequestPaymentResponse(savedPayment);
  }

  @DisplayName("processNewPayment - Mapper exception on toRequestPaymentResponse")
  @Test
  void testProcessNewPaymentMapperToResponseException() {
    PaymentEntity initialPayment = createValidPaymentEntity();
    PaymentEntity savedPayment = createValidPaymentEntity();
    savedPayment.setId(1L);
    PaymentEntity updatedPayment = createValidPaymentEntity();
    updatedPayment.setId(1L);
    updatedPayment.setGatewayPaymentId("GATEWAY-123");
    updatedPayment.setStatus("PENDING");
    updatedPayment.setMessage("Payment processed");
    GatewayPaymentDTO gatewayResponse = new GatewayPaymentDTO("GATEWAY-123", PaymentStatusDTO.PAYMENT_STATUS_FAILED, "Payment processed");

    when(paymentMapper.toPaymentEntity(validDTO, idempotencyKeyEntity)).thenReturn(initialPayment);
    when(paymentRepository.save(initialPayment)).thenReturn(savedPayment);
    when(paymentGatewayService.processPayment(validDTO)).thenReturn(gatewayResponse);
    when(paymentRepository.save(any(PaymentEntity.class))).thenReturn(updatedPayment);
    when(paymentMapper.toRequestPaymentResponse(updatedPayment))
        .thenThrow(new RuntimeException("Mapping to response failed"));

    assertThrows(RuntimeException.class, () ->
        processPaymentService.processNewPayment(validDTO, idempotencyKeyEntity));

    verify(paymentMapper).toPaymentEntity(validDTO, idempotencyKeyEntity);
    verify(paymentRepository, times(2)).save(any(PaymentEntity.class));
    verify(paymentGatewayService).processPayment(validDTO);
    verify(paymentMapper).toRequestPaymentResponse(updatedPayment);
  }

  @DisplayName("processNewPayment - Gateway service timeout exception")
  @Test
  void testProcessNewPaymentGatewayTimeoutException() {
    PaymentEntity initialPayment = createValidPaymentEntity();
    PaymentEntity savedPayment = createValidPaymentEntity();
    savedPayment.setId(1L);

    when(paymentMapper.toPaymentEntity(validDTO, idempotencyKeyEntity)).thenReturn(initialPayment);
    when(paymentRepository.save(initialPayment)).thenReturn(savedPayment);
    when(paymentGatewayService.processPayment(validDTO))
        .thenThrow(new RuntimeException("Gateway timeout"));

    assertThrows(RuntimeException.class, () ->
        processPaymentService.processNewPayment(validDTO, idempotencyKeyEntity));

    verify(paymentRepository, times(1)).save(initialPayment);
    verify(paymentGatewayService).processPayment(validDTO);
    verify(paymentMapper, times(0)).toRequestPaymentResponse(any(PaymentEntity.class));
  }

  @DisplayName("processNewPayment - Null pointer exception handling")
  @Test
  void testProcessNewPaymentNullPointerException() {
    when(paymentMapper.toPaymentEntity(validDTO, idempotencyKeyEntity))
        .thenThrow(new NullPointerException("Null entity creation"));

    assertThrows(NullPointerException.class, () ->
        processPaymentService.processNewPayment(validDTO, idempotencyKeyEntity));

    verify(paymentMapper).toPaymentEntity(validDTO, idempotencyKeyEntity);
    verify(paymentRepository, times(0)).save(any());
  }

  @DisplayName("processNewPayment - IllegalArgumentException from gateway")
  @Test
  void testProcessNewPaymentIllegalArgumentException() {
    PaymentEntity initialPayment = createValidPaymentEntity();
    PaymentEntity savedPayment = createValidPaymentEntity();
    savedPayment.setId(1L);

    when(paymentMapper.toPaymentEntity(validDTO, idempotencyKeyEntity)).thenReturn(initialPayment);
    when(paymentRepository.save(initialPayment)).thenReturn(savedPayment);
    when(paymentGatewayService.processPayment(validDTO))
        .thenThrow(new IllegalArgumentException("Invalid payment data"));

    assertThrows(IllegalArgumentException.class, () ->
        processPaymentService.processNewPayment(validDTO, idempotencyKeyEntity));

    verify(paymentRepository, times(1)).save(initialPayment);
    verify(paymentGatewayService).processPayment(validDTO);
  }



  private PaymentEntity createValidPaymentEntity() {
    PaymentEntity entity = new PaymentEntity();
    entity.setAmountMinor(5000L);
    entity.setCurrency("EUR");
    entity.setOrderId("ORDER-INT-001");
    entity.setStatus("PENDING");
    entity.setCreatedAt(LocalDateTime.now());
    entity.setIdempotencyKey(idempotencyKeyEntity);
    return entity;
  }
}
