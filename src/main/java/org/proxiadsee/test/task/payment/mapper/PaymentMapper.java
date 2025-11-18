package org.proxiadsee.test.task.payment.mapper;

import com.google.protobuf.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.proxiadsee.test.task.payment.dto.GetPaymentRequestDTO;
import org.proxiadsee.test.task.payment.dto.RequestPaymentRequestDTO;
import org.proxiadsee.test.task.payment.entity.IdempotencyKeyEntity;
import org.proxiadsee.test.task.payment.entity.PaymentEntity;
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
