package service;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.proxiadsee.test.task.payment.dto.GetPaymentRequestDTO;
import org.proxiadsee.test.task.payment.dto.RequestPaymentRequestDTO;
import org.proxiadsee.test.task.payment.mapper.PaymentMapper;
import org.proxiadsee.test.task.payment.validation.DtoValidator;
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

  @Override
  public void requestPayment(
      RequestPaymentRequest request, StreamObserver<RequestPaymentResponse> responseObserver) {
    log.info("Request payment: {}", request);

    RequestPaymentRequestDTO dto = paymentMapper.toDto(request);
    dtoValidator.validate(dto);

    log.debug("Validated DTO: {}", dto);

    responseObserver.onNext(RequestPaymentResponse.newBuilder().build());
    responseObserver.onCompleted();
  }

  @Override
  public void getPayment(
      GetPaymentRequest request, StreamObserver<GetPaymentResponse> responseObserver) {
    log.info("Get payment: {}", request);

    GetPaymentRequestDTO dto = paymentMapper.toDto(request);
    dtoValidator.validate(dto);

    log.debug("Validated DTO: {}", dto);

    responseObserver.onNext(GetPaymentResponse.newBuilder().build());
    responseObserver.onCompleted();
  }

  @Override
  public void health(HealthRequest request, StreamObserver<HealthResponse> responseObserver) {
    log.debug("Health check");
    responseObserver.onNext(OK_HEALTH_RESPONSE);
    responseObserver.onCompleted();
  }
}
