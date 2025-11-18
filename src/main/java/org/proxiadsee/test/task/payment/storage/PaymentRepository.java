package org.proxiadsee.test.task.payment.storage;

import java.util.Optional;
import org.proxiadsee.test.task.payment.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {
  Optional<PaymentEntity> findByIdempotencyKey_Id(Long idempotencyKeyId);
}
