package org.proxiadsee.interview.task.payment.storage;

import org.proxiadsee.interview.task.payment.entity.IdempotencyKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKeyEntity, Long> {
  Optional<IdempotencyKeyEntity> findByValue(String value);
}
