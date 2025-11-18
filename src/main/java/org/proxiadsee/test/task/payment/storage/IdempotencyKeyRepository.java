package storage;

import java.util.Optional;
import org.proxiadsee.test.task.domain.entity.IdempotencyKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKeyEntity, Long> {
  Optional<IdempotencyKeyEntity> findByValue(String value);
}
