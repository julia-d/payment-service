package org.proxiadsee.interview.task.payment.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.proxiadsee.interview.task.payment.domain.entity.IdempotencyKeyEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("IdempotencyKeyRepository H2 Tests")
class IdempotencyKeyRepositoryTest {

  @Autowired private IdempotencyKeyRepository idempotencyKeyRepository;

  @Test
  void saveAndFindById() {
    IdempotencyKeyEntity key = new IdempotencyKeyEntity();
    key.setValue("IDEMPOTENCY-DB-1");
    key.setRequestHash("hash-1");
    key.setCreatedAt(LocalDateTime.now());

    IdempotencyKeyEntity saved = idempotencyKeyRepository.save(key);

    Optional<IdempotencyKeyEntity> found = idempotencyKeyRepository.findById(saved.getId());

    assertTrue(found.isPresent());
    assertEquals(saved.getId(), found.get().getId());
    assertEquals("IDEMPOTENCY-DB-1", found.get().getValue());
  }

  @Test
  void saveAndFindByValue() {
    IdempotencyKeyEntity key = new IdempotencyKeyEntity();
    key.setValue("IDEMPOTENCY-DB-VALUE");
    key.setRequestHash("hash-value");
    key.setCreatedAt(LocalDateTime.now());

    idempotencyKeyRepository.save(key);

    Optional<IdempotencyKeyEntity> found =
        idempotencyKeyRepository.findByValue("IDEMPOTENCY-DB-VALUE");

    assertTrue(found.isPresent());
    assertEquals("IDEMPOTENCY-DB-VALUE", found.get().getValue());
  }
}
