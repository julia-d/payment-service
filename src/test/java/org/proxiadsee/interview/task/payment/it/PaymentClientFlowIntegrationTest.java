package org.proxiadsee.interview.task.payment.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.HashMap;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.proxiadsee.interview.task.payment.PaymentServiceApplication;
import org.proxiadsee.interview.task.payment.domain.entity.PaymentEntity;
import org.proxiadsee.interview.task.payment.storage.IdempotencyKeyRepository;
import org.proxiadsee.interview.task.payment.storage.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import payments.v1.Payment.GetPaymentRequest;
import payments.v1.Payment.GetPaymentResponse;
import payments.v1.Payment.HealthRequest;
import payments.v1.Payment.HealthResponse;
import payments.v1.Payment.PaymentStatus;
import payments.v1.Payment.RequestPaymentRequest;
import payments.v1.Payment.RequestPaymentResponse;
import payments.v1.PaymentServiceGrpc;
import payments.v1.PaymentServiceGrpc.PaymentServiceBlockingStub;

@SpringBootTest(classes = PaymentServiceApplication.class)
@ActiveProfiles("test")
class PaymentClientFlowIntegrationTest {

  private static ManagedChannel channel;
  private static PaymentServiceBlockingStub stub;

  @Autowired private PaymentRepository paymentRepository;
  @Autowired private IdempotencyKeyRepository idempotencyKeyRepository;

  @BeforeAll
  static void setUpChannel() {
    channel = ManagedChannelBuilder.forAddress("localhost", 6565).usePlaintext().build();
    stub = PaymentServiceGrpc.newBlockingStub(channel);
  }

  @BeforeEach
  void cleanDatabase() {
    paymentRepository.deleteAll();
    idempotencyKeyRepository.deleteAll();
  }

  @AfterAll
  static void tearDownChannel() {
    if (channel != null) {
      channel.shutdownNow();
    }
  }

  @Test
  void fullClientFlow() {
    HealthResponse healthResponse = stub.health(HealthRequest.newBuilder().build());
    assertNotNull(healthResponse, "Health response must not be null");
    assertEquals("OK", healthResponse.getStatus(), "Health status must be OK");

    String idempotencyKey = "it-idem-" + System.currentTimeMillis();

    // 1st payment request
    RequestPaymentRequest firstRequest =
        RequestPaymentRequest.newBuilder()
            .setAmountMinor(1000L)
            .setCurrency("USD")
            .setOrderId("ORDER-IT-1")
            .setIdempotencyKey(idempotencyKey)
            .putAllMetadata(new HashMap<>())
            .build();

    RequestPaymentResponse firstResponse = stub.requestPayment(firstRequest);

    assertThat(firstResponse).isNotNull();
    assertThat(firstResponse.getPaymentId())
        .isNotNull()
        .isNotEmpty()
        .as("Payment ID must be present");
    assertThat(firstResponse.getStatus())
        .isNotNull()
        .isNotEqualTo(PaymentStatus.PAYMENT_STATUS_UNSPECIFIED)
        .as("Payment status must be set");
    assertThat(firstResponse.getCreatedAt()).isNotNull().as("Created timestamp must be present");

    Long paymentId = Long.parseLong(firstResponse.getPaymentId());
    PaymentEntity savedEntity = paymentRepository.findById(paymentId).orElseThrow();

    assertThat(savedEntity.getId()).isNotNull().isEqualTo(paymentId);
    assertThat(savedEntity.getStatus()).isNotNull().isNotEmpty();
    assertThat(savedEntity.getGatewayPaymentId()).isNotNull().isNotEmpty();
    assertThat(savedEntity.getAmountMinor()).isEqualTo(firstRequest.getAmountMinor());
    assertThat(savedEntity.getCurrency()).isEqualTo(firstRequest.getCurrency());
    assertThat(savedEntity.getOrderId()).isEqualTo(firstRequest.getOrderId());
    assertThat(savedEntity.getCreatedAt()).isNotNull();
    assertThat(savedEntity.getIdempotencyKey()).isNotNull();
    assertThat(savedEntity.getIdempotencyKey().getValue()).isEqualTo(idempotencyKey);

    // repeated request
    RequestPaymentResponse secondResponse = stub.requestPayment(firstRequest);
    assertThat(secondResponse.getPaymentId())
        .isEqualTo(firstResponse.getPaymentId())
        .as("Idempotent call must return same payment id");
    assertThat(secondResponse.getIdempotencyKey()).isEqualTo(firstResponse.getIdempotencyKey());
    assertThat(secondResponse.getCreatedAt().getSeconds())
        .isEqualTo(firstResponse.getCreatedAt().getSeconds())
        .as("Created timestamp seconds must match for idempotent calls");

    GetPaymentResponse getPaymentResponse =
        stub.getPayment(
            GetPaymentRequest.newBuilder().setPaymentId(firstResponse.getPaymentId()).build());
    assertNotNull(getPaymentResponse, "GetPayment response must not be null");
    assertEquals(
        firstResponse.getPaymentId(),
        getPaymentResponse.getPaymentId(),
        "GetPayment must return same payment id");
    assertEquals(
        firstRequest.getAmountMinor(),
        getPaymentResponse.getAmountMinor(),
        "GetPayment must return same amount");
    assertEquals(firstRequest.getCurrency(), getPaymentResponse.getCurrency());
    assertEquals(firstRequest.getOrderId(), getPaymentResponse.getOrderId());
    assertEquals(idempotencyKey, getPaymentResponse.getIdempotencyKey());

    RequestPaymentRequest conflictingRequest =
        RequestPaymentRequest.newBuilder()
            .setAmountMinor(2000L)
            .setCurrency("USD")
            .setOrderId("ORDER-IT-1")
            .setIdempotencyKey(idempotencyKey)
            .putAllMetadata(new HashMap<>())
            .build();

    StatusRuntimeException exception =
        assertThrows(
            StatusRuntimeException.class,
            () -> stub.requestPayment(conflictingRequest),
            "Expected conflict on changed payload with same idempotency key");

    assertThat(exception.getStatus().getCode())
        .isEqualTo(Status.Code.INVALID_ARGUMENT);
  }
}
