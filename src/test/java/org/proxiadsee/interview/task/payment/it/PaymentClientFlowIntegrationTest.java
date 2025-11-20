package org.proxiadsee.interview.task.payment.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import java.util.HashMap;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.proxiadsee.interview.task.payment.PaymentServiceApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import payments.v1.Payment.GetPaymentRequest;
import payments.v1.Payment.GetPaymentResponse;
import payments.v1.Payment.HealthRequest;
import payments.v1.Payment.HealthResponse;
import payments.v1.Payment.RequestPaymentRequest;
import payments.v1.Payment.RequestPaymentResponse;
import payments.v1.PaymentServiceGrpc;
import payments.v1.PaymentServiceGrpc.PaymentServiceBlockingStub;

@SpringBootTest(classes = PaymentServiceApplication.class)
@ActiveProfiles("test")
class PaymentClientFlowIntegrationTest {

  private static ManagedChannel channel;
  private static PaymentServiceBlockingStub stub;

  @BeforeAll
  static void setUpChannel() {
    channel = ManagedChannelBuilder.forAddress("localhost", 6565).usePlaintext().build();
    stub = PaymentServiceGrpc.newBlockingStub(channel);
  }

  @AfterAll
  static void tearDownChannel() {
    if (channel != null) {
      channel.shutdownNow();
    }
  }

  void fullClientFlow() {
    HealthResponse healthResponse = stub.health(HealthRequest.newBuilder().build());
    assertNotNull(healthResponse, "Health response must not be null");
    assertEquals("OK", healthResponse.getStatus(), "Health status must be OK");

    String idempotencyKey = "it-idem-" + System.currentTimeMillis();

    RequestPaymentRequest firstRequest =
        RequestPaymentRequest.newBuilder()
            .setAmountMinor(1000L)
            .setCurrency("USD")
            .setOrderId("ORDER-IT-1")
            .setIdempotencyKey(idempotencyKey)
            .putAllMetadata(new HashMap<>())
            .build();

    RequestPaymentResponse firstResponse = stub.requestPayment(firstRequest);
    assertNotNull(firstResponse, "First payment response must not be null");
    assertNotNull(firstResponse.getPaymentId(), "First payment id must not be null");

    RequestPaymentResponse secondResponse = stub.requestPayment(firstRequest);
    assertEquals(
        firstResponse.getPaymentId(),
        secondResponse.getPaymentId(),
        "Idempotent call must return same payment id");
    assertEquals(firstResponse, secondResponse, "Responses for same idempotent request must match");

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

    try {
      stub.requestPayment(conflictingRequest);
      throw new AssertionError("Expected conflict on changed payload with same idempotency key");
    } catch (StatusRuntimeException ex) {
      if (!ex.getStatus().getCode().equals(io.grpc.Status.Code.ABORTED)
          && !ex.getStatus().getCode().equals(io.grpc.Status.Code.ALREADY_EXISTS)
          && !ex.getStatus().getCode().equals(io.grpc.Status.Code.FAILED_PRECONDITION)
          && !ex.getStatus().getCode().equals(io.grpc.Status.Code.INVALID_ARGUMENT)) {
        throw new AssertionError(
            "Unexpected status for idempotency conflict: " + ex.getStatus(), ex);
      }
    }
  }
}
