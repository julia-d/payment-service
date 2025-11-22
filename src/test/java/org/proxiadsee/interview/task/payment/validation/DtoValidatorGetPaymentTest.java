package org.proxiadsee.interview.task.payment.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.proxiadsee.interview.task.payment.domain.dto.GetPaymentRequestDTO;
import org.proxiadsee.interview.task.payment.exception.ValidationException;

@DisplayName("DtoValidator - GetPaymentRequestDTO Tests")
class DtoValidatorGetPaymentTest {

  private DtoValidator dtoValidator;
  private Validator validator;

  @BeforeEach
  void setUp() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
    dtoValidator = new DtoValidator(validator);
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("validGetPaymentDtoProvider")
  @DisplayName("Should pass validation for valid GetPaymentRequestDTO")
  void shouldPassValidationForValidDto(
      String testCase, GetPaymentRequestDTO dto, String expectedResult) {
    assertDoesNotThrow(
        () -> dtoValidator.validate(dto), "Expected validation to pass for: " + testCase);
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("invalidGetPaymentDtoProvider")
  @DisplayName("Should fail validation for invalid GetPaymentRequestDTO")
  void shouldFailValidationForInvalidDto(
      String testCase, GetPaymentRequestDTO dto, String expectedErrorField) {
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

  static Stream<Arguments> validGetPaymentDtoProvider() {
    return Stream.of(
        Arguments.of("Valid DTO with single digit", new GetPaymentRequestDTO("1"), "success"),
        Arguments.of(
            "Valid DTO with multiple digits", new GetPaymentRequestDTO("12345"), "success"),
        Arguments.of(
            "Valid DTO with large number", new GetPaymentRequestDTO("999999999999999"), "success"),
        Arguments.of("Valid DTO with leading zeros", new GetPaymentRequestDTO("00123"), "success"));
  }

  static Stream<Arguments> invalidGetPaymentDtoProvider() {
    return Stream.of(
        Arguments.of("Invalid: null paymentId", new GetPaymentRequestDTO(null), "paymentId"),
        Arguments.of("Invalid: empty paymentId", new GetPaymentRequestDTO(""), "paymentId"),
        Arguments.of("Invalid: blank paymentId", new GetPaymentRequestDTO("   "), "paymentId"),
        Arguments.of(
            "Invalid: alphanumeric paymentId", new GetPaymentRequestDTO("abc123"), "paymentId"),
        Arguments.of(
            "Invalid: alphabetic paymentId", new GetPaymentRequestDTO("abcdef"), "paymentId"),
        Arguments.of(
            "Invalid: paymentId with special characters",
            new GetPaymentRequestDTO("123-456"),
            "paymentId"),
        Arguments.of(
            "Invalid: paymentId with spaces", new GetPaymentRequestDTO("123 456"), "paymentId"),
        Arguments.of(
            "Invalid: paymentId with decimal", new GetPaymentRequestDTO("123.45"), "paymentId"),
        Arguments.of(
            "Invalid: negative number (with minus sign)",
            new GetPaymentRequestDTO("-123"),
            "paymentId"),
        Arguments.of(
            "Invalid: paymentId with plus sign", new GetPaymentRequestDTO("+123"), "paymentId"));
  }
}
