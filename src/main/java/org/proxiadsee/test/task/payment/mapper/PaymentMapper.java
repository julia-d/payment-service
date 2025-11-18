package org.proxiadsee.test.task.payment.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.proxiadsee.test.task.payment.dto.GetPaymentRequestDTO;
import org.proxiadsee.test.task.payment.dto.RequestPaymentRequestDTO;
import payments.v1.Payment.GetPaymentRequest;
import payments.v1.Payment.RequestPaymentRequest;

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
}
