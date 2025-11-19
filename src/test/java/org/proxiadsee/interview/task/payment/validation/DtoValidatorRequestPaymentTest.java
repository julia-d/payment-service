package org.proxiadsee.interview.task.payment.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.proxiadsee.interview.task.payment.domain.dto.RequestPaymentRequestDTO;
import org.proxiadsee.interview.task.payment.exception.ValidationException;

@DisplayName("DtoValidator - RequestPaymentRequestDTO Tests")
class DtoValidatorRequestPaymentTest {

  private DtoValidator dtoValidator;
  private Validator validator;

  @BeforeEach
  void setUp() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
    dtoValidator = new DtoValidator(validator);
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("validRequestPaymentDtoProvider")
  @DisplayName("Should pass validation for valid RequestPaymentRequestDTO")
  void shouldPassValidationForValidDto(
      String testCase, RequestPaymentRequestDTO dto, String expectedResult) {
    assertDoesNotThrow(
        () -> dtoValidator.validate(dto), "Expected validation to pass for: " + testCase);
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("invalidRequestPaymentDtoProvider")
  @DisplayName("Should fail validation for invalid RequestPaymentRequestDTO")
  void shouldFailValidationForInvalidDto(
      String testCase, RequestPaymentRequestDTO dto, String expectedErrorField) {
    ValidationException exception =
        assertThrows(
            ValidationException.class,
            () -> dtoValidator.validate(dto),
            "Expected validation to fail for: " + testCase);

    assertTrue(
        exception.getMessage().contains(expectedErrorField),
        "Expected error message to contain field: "
            + expectedErrorField
            + ", but got: "
            + exception.getMessage());
  }

  static Stream<Arguments> validRequestPaymentDtoProvider() {
    Map<String, String> metadata = new HashMap<>();
    metadata.put("key1", "value1");

    return Stream.of(
        Arguments.of(
            "Valid DTO with all fields",
            new RequestPaymentRequestDTO(1000L, "USD", "order-123", "idempotency-key-1", metadata),
            "success"),
        Arguments.of(
            "Valid DTO with EUR currency",
            new RequestPaymentRequestDTO(5000L, "EUR", "order-456", "idempotency-key-2", null),
            "success"),
        Arguments.of(
            "Valid DTO with GBP currency",
            new RequestPaymentRequestDTO(250L, "GBP", "order-789", "idempotency-key-3", metadata),
            "success"),
        Arguments.of(
            "Valid DTO with minimum amount (1)",
            new RequestPaymentRequestDTO(1L, "USD", "order-min", "idempotency-key-min", null),
            "success"),
        Arguments.of(
            "Valid DTO with large amount",
            new RequestPaymentRequestDTO(
                999999999L, "JPY", "order-max", "idempotency-key-max", metadata),
            "success"),
        Arguments.of(
            "Valid DTO with empty metadata",
            new RequestPaymentRequestDTO(
                100L, "CAD", "order-empty-meta", "idempotency-key-empty", new HashMap<>()),
            "success"));
  }

  static Stream<Arguments> invalidRequestPaymentDtoProvider() {
    Map<String, String> metadata = new HashMap<>();
    metadata.put("key1", "value1");

    return Stream.of(
        Arguments.of(
            "Invalid: zero amount",
            new RequestPaymentRequestDTO(0L, "USD", "order-123", "idempotency-key-1", metadata),
            "amountMinor"),
        Arguments.of(
            "Invalid: negative amount",
            new RequestPaymentRequestDTO(-100L, "USD", "order-123", "idempotency-key-1", metadata),
            "amountMinor"),
        Arguments.of(
            "Invalid: null currency",
            new RequestPaymentRequestDTO(1000L, null, "order-123", "idempotency-key-1", metadata),
            "currency"),
        Arguments.of(
            "Invalid: empty currency",
            new RequestPaymentRequestDTO(1000L, "", "order-123", "idempotency-key-1", metadata),
            "currency"),
        Arguments.of(
            "Invalid: lowercase currency",
            new RequestPaymentRequestDTO(1000L, "usd", "order-123", "idempotency-key-1", metadata),
            "currency"),
        Arguments.of(
            "Invalid: currency too short",
            new RequestPaymentRequestDTO(1000L, "US", "order-123", "idempotency-key-1", metadata),
            "currency"),
        Arguments.of(
            "Invalid: currency too long",
            new RequestPaymentRequestDTO(1000L, "USDD", "order-123", "idempotency-key-1", metadata),
            "currency"),
        Arguments.of(
            "Invalid: currency with numbers",
            new RequestPaymentRequestDTO(1000L, "U5D", "order-123", "idempotency-key-1", metadata),
            "currency"),
        Arguments.of(
            "Invalid: null orderId",
            new RequestPaymentRequestDTO(1000L, "USD", null, "idempotency-key-1", metadata),
            "orderId"),
        Arguments.of(
            "Invalid: empty orderId",
            new RequestPaymentRequestDTO(1000L, "USD", "", "idempotency-key-1", metadata),
            "orderId"),
        Arguments.of(
            "Invalid: blank orderId",
            new RequestPaymentRequestDTO(1000L, "USD", "   ", "idempotency-key-1", metadata),
            "orderId"),
        Arguments.of(
            "Invalid: null idempotencyKey",
            new RequestPaymentRequestDTO(1000L, "USD", "order-123", null, metadata),
            "idempotencyKey"),
        Arguments.of(
            "Invalid: empty idempotencyKey",
            new RequestPaymentRequestDTO(1000L, "USD", "order-123", "", metadata),
            "idempotencyKey"),
        Arguments.of(
            "Invalid: blank idempotencyKey",
            new RequestPaymentRequestDTO(1000L, "USD", "order-123", "   ", metadata),
            "idempotencyKey"),
        Arguments.of(
            "Invalid: multiple violations (negative amount + invalid currency)",
            new RequestPaymentRequestDTO(-100L, "usd", "order-123", "idempotency-key-1", metadata),
            "amountMinor"));
  }
}
