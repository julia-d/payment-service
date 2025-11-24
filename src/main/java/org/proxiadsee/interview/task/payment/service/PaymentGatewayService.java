package org.proxiadsee.interview.task.payment.service;

import lombok.extern.slf4j.Slf4j;
import org.proxiadsee.interview.task.payment.domain.dto.GatewayPaymentDTO;
import org.proxiadsee.interview.task.payment.domain.dto.PaymentStatusDTO;
import org.proxiadsee.interview.task.payment.domain.dto.RequestPaymentRequestDTO;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PaymentGatewayService {
  private static int counter = 0;

  public GatewayPaymentDTO processPayment(RequestPaymentRequestDTO dto) {
    log.info("processPayment: {}", dto);
    return new GatewayPaymentDTO(
        "STUB-GATEWAY-ID" + counter++, PaymentStatusDTO.PAYMENT_STATUS_SUCCEEDED, "stub");
  }
}
