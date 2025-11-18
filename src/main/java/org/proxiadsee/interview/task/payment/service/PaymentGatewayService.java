package org.proxiadsee.interview.task.payment.service;

import lombok.extern.slf4j.Slf4j;
import org.proxiadsee.interview.task.payment.dto.GatewayPaymentDTO;
import org.proxiadsee.interview.task.payment.dto.PaymentStatusDTO;
import org.proxiadsee.interview.task.payment.dto.RequestPaymentRequestDTO;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PaymentGatewayService {

  public GatewayPaymentDTO processPayment(RequestPaymentRequestDTO dto) {
    log.info("Processing payment with gateway for order: {}", dto.orderId());
    return new GatewayPaymentDTO(
        "gateway-generated-id", PaymentStatusDTO.PAYMENT_STATUS_PENDING, "");
  }
}
