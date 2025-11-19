package org.proxiadsee.interview.task.payment.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.proxiadsee.interview.task.payment.domain.entity.IdempotencyKeyEntity;
import org.proxiadsee.interview.task.payment.domain.entity.PaymentEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("PaymentRepository H2 Tests")
class PaymentRepositoryTest {

  @Autowired private PaymentRepository paymentRepository;

  @Autowired private IdempotencyKeyRepository idempotencyKeyRepository;

  @Test
  void saveAndFindByIdAndByIdempotencyKey() {
    IdempotencyKeyEntity key = new IdempotencyKeyEntity();
    key.setValue("IDEMPOTENCY-DB-2");
    key.setRequestHash("hash-2");
    key.setCreatedAt(LocalDateTime.now());

    IdempotencyKeyEntity savedKey = idempotencyKeyRepository.save(key);

    PaymentEntity payment = new PaymentEntity();
    payment.setIdempotencyKey(savedKey);
    payment.setGatewayPaymentId("GW-1");
    payment.setAmountMinor(100L);
    payment.setCurrency("EUR");
    payment.setStatus("PENDING");
    payment.setOrderId("ORDER-1");
    payment.setCreatedAt(LocalDateTime.now());
    payment.setMetadata("meta");
    payment.setMessage("msg");

    PaymentEntity savedPayment = paymentRepository.save(payment);

    Optional<PaymentEntity> foundById = paymentRepository.findById(savedPayment.getId());
    assertTrue(foundById.isPresent());
    assertEquals(savedPayment.getId(), foundById.get().getId());

    Optional<PaymentEntity> foundByIdempotency =
        paymentRepository.findByIdempotencyKey_Id(savedKey.getId());
    assertTrue(foundByIdempotency.isPresent());
    assertEquals(savedPayment.getId(), foundByIdempotency.get().getId());
  }
}
