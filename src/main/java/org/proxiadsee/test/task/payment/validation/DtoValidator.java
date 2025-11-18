package org.proxiadsee.test.task.payment.validation;

import io.grpc.Status;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DtoValidator {

  private final Validator validator;

  public <T> void validate(T dto) {
    Set<ConstraintViolation<T>> violations = validator.validate(dto);
    if (!violations.isEmpty()) {
      String errorMessage =
          violations.stream()
              .map(v -> v.getPropertyPath() + ": " + v.getMessage())
              .collect(Collectors.joining(", "));
      throw Status.INVALID_ARGUMENT.withDescription(errorMessage).asRuntimeException();
    }
  }
}
