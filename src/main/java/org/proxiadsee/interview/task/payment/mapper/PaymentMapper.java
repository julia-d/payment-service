package org.proxiadsee.interview.task.payment.mapper;

import com.google.protobuf.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.proxiadsee.interview.task.payment.dto.GetPaymentRequestDTO;
import org.proxiadsee.interview.task.payment.dto.RequestPaymentRequestDTO;
import org.proxiadsee.interview.task.payment.entity.IdempotencyKeyEntity;
import org.proxiadsee.interview.task.payment.entity.PaymentEntity;
import payments.v1.Payment.GetPaymentRequest;
import payments.v1.Payment.GetPaymentResponse;
import payments.v1.Payment.PaymentStatus;
import payments.v1.Payment.RequestPaymentRequest;
import payments.v1.Payment.RequestPaymentResponse;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

  @Mapping(target = "amountMinor", source = "amountMinor")
  @Mapping(target = "currency", source = "currency")
  @Mapping(target = "orderId", source = "orderId")
  @Mapping(target = "idempotencyKey", source = "idempotencyKey")
  @Mapping(target = "metadata", source = "metadataMap")
  RequestPaymentRequestDTO toDto(RequestPaymentRequest request);

  @Mapping(target = "paymentId", source = "paymentId")
  GetPaymentRequestDTO toDto(GetPaymentRequest request);

  @Mapping(target = "paymentId", expression = "java(String.valueOf(entity.getId()))")
  @Mapping(target = "status", expression = "java(mapPaymentStatus(entity.getStatus()))")
  @Mapping(target = "amountMinor", source = "amountMinor")
  @Mapping(target = "currency", source = "currency")
  @Mapping(target = "orderId", source = "orderId")
  @Mapping(target = "idempotencyKey", expression = "java(entity.getIdempotencyKey().getValue())")
  @Mapping(
      target = "createdAt",
      expression = "java(mapLocalDateTimeToTimestamp(entity.getCreatedAt()))")
  @Mapping(
      target = "message",
      expression = "java(entity.getMessage() == null ? \"\" : entity.getMessage())")
  GetPaymentResponse toGetPaymentResponse(PaymentEntity entity);

  @Mapping(target = "paymentId", expression = "java(String.valueOf(entity.getId()))")
  @Mapping(target = "status", expression = "java(mapPaymentStatus(entity.getStatus()))")
  @Mapping(
      target = "message",
      expression = "java(entity.getMessage() == null ? \"\" : entity.getMessage())")
  @Mapping(target = "idempotencyKey", expression = "java(entity.getIdempotencyKey().getValue())")
  @Mapping(
      target = "createdAt",
      expression = "java(mapLocalDateTimeToTimestamp(entity.getCreatedAt()))")
  RequestPaymentResponse toRequestPaymentResponse(PaymentEntity entity);

  @Mapping(target = "paymentId", constant = "")
  @Mapping(target = "status", constant = "PAYMENT_STATUS_UNSPECIFIED")
  @Mapping(target = "message", constant = "")
  @Mapping(target = "idempotencyKey", source = "dto.idempotencyKey")
  @Mapping(
      target = "createdAt",
      source = "idempotencyKeyEntity.createdAt",
      qualifiedByName = "toTimestamp")
  RequestPaymentResponse toRequestPaymentResponse(
      RequestPaymentRequestDTO dto, IdempotencyKeyEntity idempotencyKeyEntity);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "idempotencyKey", source = "idempotencyKeyEntity")
  @Mapping(target = "gatewayPaymentId", ignore = true)
  @Mapping(target = "amountMinor", source = "dto.amountMinor")
  @Mapping(target = "currency", source = "dto.currency")
  @Mapping(target = "status", constant = "PAYMENT_STATUS_PENDING")
  @Mapping(target = "orderId", source = "dto.orderId")
  @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
  @Mapping(target = "metadata", ignore = true)
  @Mapping(target = "message", ignore = true)
  PaymentEntity toPaymentEntity(
      RequestPaymentRequestDTO dto, IdempotencyKeyEntity idempotencyKeyEntity);

  default PaymentStatus mapPaymentStatus(String status) {
    try {
      return PaymentStatus.valueOf(status);
    } catch (IllegalArgumentException ex) {
      return PaymentStatus.PAYMENT_STATUS_UNSPECIFIED;
    }
  }

  default Timestamp mapLocalDateTimeToTimestamp(LocalDateTime dateTime) {
    long seconds = dateTime.toInstant(ZoneOffset.UTC).getEpochSecond();
    int nanos = dateTime.getNano();
    return Timestamp.newBuilder().setSeconds(seconds).setNanos(nanos).build();
  }

  @org.mapstruct.Named("toTimestamp")
  default Timestamp toTimestamp(LocalDateTime dateTime) {
    if (dateTime == null) {
      return Timestamp.newBuilder().build();
    }
    long seconds = dateTime.toInstant(ZoneOffset.UTC).getEpochSecond();
    int nanos = dateTime.getNano();
    return Timestamp.newBuilder().setSeconds(seconds).setNanos(nanos).build();
  }
}
