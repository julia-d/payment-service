package org.proxiadsee.interview.task.payment.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.grpc.stub.StreamObserver;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.proxiadsee.interview.task.payment.domain.dto.GetPaymentRequestDTO;
import org.proxiadsee.interview.task.payment.domain.dto.RequestPaymentRequestDTO;
import org.proxiadsee.interview.task.payment.domain.entity.IdempotencyKeyEntity;
import org.proxiadsee.interview.task.payment.domain.entity.PaymentEntity;
import org.proxiadsee.interview.task.payment.exception.ServiceException;
import org.proxiadsee.interview.task.payment.mapper.PaymentMapper;
import org.proxiadsee.interview.task.payment.storage.IdempotencyKeyRepository;
import org.proxiadsee.interview.task.payment.storage.PaymentRepository;
import org.proxiadsee.interview.task.payment.validation.DtoValidator;
import payments.v1.Payment.GetPaymentRequest;
import payments.v1.Payment.GetPaymentResponse;
import payments.v1.Payment.HealthRequest;
import payments.v1.Payment.HealthResponse;
import payments.v1.Payment.RequestPaymentRequest;
import payments.v1.Payment.RequestPaymentResponse;

@DisplayName("PaymentService Tests")
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

  @Mock private PaymentMapper paymentMapper;

  @Mock private DtoValidator dtoValidator;

  @Mock private IdempotencyKeyRepository idempotencyKeyRepository;

  @Mock private ProcessPaymentService processPaymentService;

  @Mock private PaymentRepository paymentRepository;

  private PaymentService paymentService;

  @BeforeEach
  void setUp() {
    paymentService =
        new PaymentService(
            paymentMapper,
            dtoValidator,
            idempotencyKeyRepository,
            processPaymentService,
            paymentRepository);
  }

  @DisplayName("requestPayment - Happy path with new idempotency key")
  @Test
  void testRequestPaymentWithNewIdempotencyKey() {
    RequestPaymentRequest request = createValidRequestPaymentRequest();
    RequestPaymentRequestDTO dto = createValidRequestPaymentDTO();
    RequestPaymentResponse expectedResponse = createValidRequestPaymentResponse();
    IdempotencyKeyEntity mappedIdempotency = createValidIdempotencyKeyEntity();

    when(paymentMapper.toDto(request)).thenReturn(dto);
    when(idempotencyKeyRepository.findByValue(dto.idempotencyKey())).thenReturn(Optional.empty());
    when(paymentMapper.toIdempotencyKey(dto)).thenReturn(mappedIdempotency);
    when(processPaymentService.processNewPayment(dto, mappedIdempotency))
        .thenReturn(expectedResponse);

    @SuppressWarnings("unchecked")
    StreamObserver<RequestPaymentResponse> responseObserver = mock(StreamObserver.class);

    paymentService.requestPayment(request, responseObserver);

    ArgumentCaptor<RequestPaymentResponse> captor =
        ArgumentCaptor.forClass(RequestPaymentResponse.class);
    verify(responseObserver).onNext(captor.capture());
    verify(responseObserver).onCompleted();
    verify(idempotencyKeyRepository).save(mappedIdempotency);
    assertEquals(expectedResponse, captor.getValue());
  }

  @DisplayName("requestPayment - Happy path with existing idempotency key")
  @Test
  void testRequestPaymentWithExistingIdempotencyKey() {
    RequestPaymentRequest request = createValidRequestPaymentRequest();
    RequestPaymentRequestDTO dto = createValidRequestPaymentDTO();
    IdempotencyKeyEntity existingEntity = createValidIdempotencyKeyEntity();
    RequestPaymentResponse expectedResponse = createValidRequestPaymentResponse();

    when(paymentMapper.toDto(request)).thenReturn(dto);
    when(idempotencyKeyRepository.findByValue(dto.idempotencyKey()))
        .thenReturn(Optional.of(existingEntity));
    when(processPaymentService.processExistingPayment(dto, existingEntity))
        .thenReturn(expectedResponse);

    @SuppressWarnings("unchecked")
    StreamObserver<RequestPaymentResponse> responseObserver = mock(StreamObserver.class);

    paymentService.requestPayment(request, responseObserver);

    ArgumentCaptor<RequestPaymentResponse> captor =
        ArgumentCaptor.forClass(RequestPaymentResponse.class);
    verify(responseObserver).onNext(captor.capture());
    verify(responseObserver).onCompleted();
    verify(processPaymentService, never()).processNewPayment(any(), any());
    verify(idempotencyKeyRepository, never()).save(any());
    assertEquals(expectedResponse, captor.getValue());
  }

  @DisplayName("requestPayment - Validation failure throws and does not hit repository")
  @Test
  void testRequestPaymentValidationFailure() {
    RequestPaymentRequest request = createValidRequestPaymentRequest();
    RequestPaymentRequestDTO dto = createValidRequestPaymentDTO();

    when(paymentMapper.toDto(request)).thenReturn(dto);
    doThrow(new RuntimeException("Validation failed")).when(dtoValidator).validate(dto);

    @SuppressWarnings("unchecked")
    StreamObserver<RequestPaymentResponse> responseObserver = mock(StreamObserver.class);

    assertThrows(
        RuntimeException.class, () -> paymentService.requestPayment(request, responseObserver));

    verify(responseObserver, never()).onNext(any());
    verify(responseObserver, never()).onError(any());
    verify(idempotencyKeyRepository, never()).findByValue(anyString());
    verify(processPaymentService, never()).processExistingPayment(any(), any());
    verify(processPaymentService, never()).processNewPayment(any(), any());
  }

  @DisplayName("requestPayment - Creates new IdempotencyKeyEntity via mapper and saves it")
  @Test
  void testRequestPaymentCreatesNewIdempotencyKeyEntity() {
    RequestPaymentRequest request = createValidRequestPaymentRequest();
    RequestPaymentRequestDTO dto = createValidRequestPaymentDTO();
    RequestPaymentResponse expectedResponse = createValidRequestPaymentResponse();
    IdempotencyKeyEntity mappedIdempotency = createValidIdempotencyKeyEntity();

    when(paymentMapper.toDto(request)).thenReturn(dto);
    when(idempotencyKeyRepository.findByValue(dto.idempotencyKey())).thenReturn(Optional.empty());
    when(paymentMapper.toIdempotencyKey(dto)).thenReturn(mappedIdempotency);
    when(processPaymentService.processNewPayment(dto, mappedIdempotency))
        .thenReturn(expectedResponse);

    @SuppressWarnings("unchecked")
    StreamObserver<RequestPaymentResponse> responseObserver = mock(StreamObserver.class);

    paymentService.requestPayment(request, responseObserver);

    verify(paymentMapper).toIdempotencyKey(dto);
    verify(idempotencyKeyRepository).save(mappedIdempotency);
  }

  @DisplayName(
      "requestPayment - processNewPayment failure triggers idempotency key deletion and exception")
  @Test
  void testRequestPaymentProcessNewPaymentFailureDeletesIdempotencyKey() {
    RequestPaymentRequest request = createValidRequestPaymentRequest();
    RequestPaymentRequestDTO dto = createValidRequestPaymentDTO();
    IdempotencyKeyEntity mappedIdempotency = createValidIdempotencyKeyEntity();

    when(paymentMapper.toDto(request)).thenReturn(dto);
    when(idempotencyKeyRepository.findByValue(dto.idempotencyKey())).thenReturn(Optional.empty());
    when(paymentMapper.toIdempotencyKey(dto)).thenReturn(mappedIdempotency);
    when(processPaymentService.processNewPayment(dto, mappedIdempotency))
        .thenThrow(new RuntimeException("Processing failed"));

    @SuppressWarnings("unchecked")
    StreamObserver<RequestPaymentResponse> responseObserver = mock(StreamObserver.class);

    assertThrows(
        ServiceException.class, () -> paymentService.requestPayment(request, responseObserver));

    verify(idempotencyKeyRepository).save(mappedIdempotency);
    verify(idempotencyKeyRepository).delete(mappedIdempotency);
    verify(responseObserver, never()).onNext(any());
    verify(responseObserver, never()).onError(any());
  }

  @DisplayName("getPayment - Happy path with existing payment")
  @Test
  void testGetPaymentWithExistingPayment() {
    GetPaymentRequest request = createValidGetPaymentRequest();
    GetPaymentRequestDTO dto = createValidGetPaymentRequestDTO();
    PaymentEntity paymentEntity = createValidPaymentEntity();
    GetPaymentResponse expectedResponse = createValidGetPaymentResponse();

    when(paymentMapper.toDto(request)).thenReturn(dto);
    when(paymentRepository.findById(1L)).thenReturn(Optional.of(paymentEntity));
    when(paymentMapper.toGetPaymentResponse(paymentEntity)).thenReturn(expectedResponse);

    @SuppressWarnings("unchecked")
    StreamObserver<GetPaymentResponse> responseObserver = mock(StreamObserver.class);

    paymentService.getPayment(request, responseObserver);

    ArgumentCaptor<GetPaymentResponse> captor = ArgumentCaptor.forClass(GetPaymentResponse.class);
    verify(responseObserver).onNext(captor.capture());
    verify(responseObserver).onCompleted();
    assertEquals(expectedResponse, captor.getValue());
  }

  @DisplayName("getPayment - Payment not found")
  @Test
  void testGetPaymentNotFound() {
    GetPaymentRequest request = createValidGetPaymentRequest();
    GetPaymentRequestDTO dto = createValidGetPaymentRequestDTO();

    when(paymentMapper.toDto(request)).thenReturn(dto);
    when(paymentRepository.findById(1L)).thenReturn(Optional.empty());

    @SuppressWarnings("unchecked")
    StreamObserver<GetPaymentResponse> responseObserver = mock(StreamObserver.class);

    paymentService.getPayment(request, responseObserver);

    ArgumentCaptor<GetPaymentResponse> captor = ArgumentCaptor.forClass(GetPaymentResponse.class);
    verify(responseObserver).onNext(captor.capture());
    verify(responseObserver).onCompleted();
    GetPaymentResponse response = captor.getValue();
    assertNotNull(response);
  }

  @DisplayName("getPayment - Validation failure")
  @Test
  void testGetPaymentValidationFailure() {
    GetPaymentRequest request = createValidGetPaymentRequest();
    GetPaymentRequestDTO dto = createValidGetPaymentRequestDTO();

    when(paymentMapper.toDto(request)).thenReturn(dto);
    doThrow(new RuntimeException("Validation failed")).when(dtoValidator).validate(dto);

    @SuppressWarnings("unchecked")
    StreamObserver<GetPaymentResponse> responseObserver = mock(StreamObserver.class);

    assertThrows(
        RuntimeException.class, () -> paymentService.getPayment(request, responseObserver));
    verify(responseObserver, never()).onNext(any());
    verify(paymentRepository, never()).findById(any());
  }

  @DisplayName("getPayment - Edge case: numeric payment ID conversion")
  @ParameterizedTest(name = "Payment ID: {0}")
  @ValueSource(strings = {"1", "999", "9223372036854775807"})
  void testGetPaymentWithVariousNumericIds(String paymentId) {
    GetPaymentRequest request = GetPaymentRequest.newBuilder().setPaymentId(paymentId).build();
    GetPaymentRequestDTO dto = new GetPaymentRequestDTO(paymentId);
    PaymentEntity paymentEntity = createValidPaymentEntity();
    GetPaymentResponse expectedResponse = createValidGetPaymentResponse();

    when(paymentMapper.toDto(request)).thenReturn(dto);
    Long expectedId = Long.parseLong(paymentId);
    when(paymentRepository.findById(expectedId)).thenReturn(Optional.of(paymentEntity));
    when(paymentMapper.toGetPaymentResponse(paymentEntity)).thenReturn(expectedResponse);

    @SuppressWarnings("unchecked")
    StreamObserver<GetPaymentResponse> responseObserver = mock(StreamObserver.class);

    paymentService.getPayment(request, responseObserver);

    verify(paymentRepository).findById(expectedId);
    verify(responseObserver).onCompleted();
  }

  @DisplayName("health - Should return OK status")
  @Test
  void testHealthCheck() {
    HealthRequest request = HealthRequest.newBuilder().build();

    @SuppressWarnings("unchecked")
    StreamObserver<HealthResponse> responseObserver = mock(StreamObserver.class);

    paymentService.health(request, responseObserver);

    ArgumentCaptor<HealthResponse> captor = ArgumentCaptor.forClass(HealthResponse.class);
    verify(responseObserver).onNext(captor.capture());
    verify(responseObserver).onCompleted();

    HealthResponse response = captor.getValue();
    assertEquals("OK", response.getStatus());
  }

  @DisplayName("health - Multiple health checks should return consistent response")
  @Test
  void testMultipleHealthChecks() {
    HealthRequest request = HealthRequest.newBuilder().build();

    for (int i = 0; i < 3; i++) {
      @SuppressWarnings("unchecked")
      StreamObserver<HealthResponse> responseObserver = mock(StreamObserver.class);

      paymentService.health(request, responseObserver);

      ArgumentCaptor<HealthResponse> captor = ArgumentCaptor.forClass(HealthResponse.class);
      verify(responseObserver).onNext(captor.capture());
      assertEquals("OK", captor.getValue().getStatus());
    }
  }

  private RequestPaymentRequest createValidRequestPaymentRequest() {
    return RequestPaymentRequest.newBuilder()
        .setAmountMinor(1000)
        .setCurrency("USD")
        .setOrderId("ORDER123")
        .setIdempotencyKey("IDEMPOTENT-KEY-123")
        .build();
  }

  private RequestPaymentRequestDTO createValidRequestPaymentDTO() {
    return new RequestPaymentRequestDTO(
        1000L, "USD", "ORDER123", "IDEMPOTENT-KEY-123", new HashMap<>());
  }

  private RequestPaymentResponse createValidRequestPaymentResponse() {
    return RequestPaymentResponse.newBuilder().setPaymentId("1").build();
  }

  private IdempotencyKeyEntity createValidIdempotencyKeyEntity() {
    IdempotencyKeyEntity entity = new IdempotencyKeyEntity();
    entity.setId(1L);
    entity.setValue("IDEMPOTENT-KEY-123");
    entity.setRequestHash("somehash");
    entity.setCreatedAt(LocalDateTime.now());
    return entity;
  }

  private GetPaymentRequest createValidGetPaymentRequest() {
    return GetPaymentRequest.newBuilder().setPaymentId("1").build();
  }

  private GetPaymentRequestDTO createValidGetPaymentRequestDTO() {
    return new GetPaymentRequestDTO("1");
  }

  private GetPaymentResponse createValidGetPaymentResponse() {
    return GetPaymentResponse.newBuilder().setPaymentId("1").build();
  }

  private PaymentEntity createValidPaymentEntity() {
    PaymentEntity entity = new PaymentEntity();
    entity.setId(1L);
    entity.setAmountMinor(1000L);
    entity.setCurrency("USD");
    entity.setOrderId("ORDER123");
    entity.setStatus("PENDING");
    entity.setCreatedAt(LocalDateTime.now());
    return entity;
  }
}
