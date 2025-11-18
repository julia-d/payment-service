package org.proxiadsee.interview.task.payment.service;

import org.proxiadsee.interview.task.payment.domain.dto.GatewayPaymentDTO;
import org.proxiadsee.interview.task.payment.domain.dto.RequestPaymentRequestDTO;

public interface PaymentGatewayService {
  GatewayPaymentDTO processPayment(RequestPaymentRequestDTO dto);
}
