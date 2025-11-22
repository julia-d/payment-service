package org.proxiadsee.interview.task.payment.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class IdempotencyKeyHashingServiceTest {

  private IdempotencyKeyHashingService hashingService;

  @BeforeEach
  void setUp() {
    hashingService = new IdempotencyKeyHashingService();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "simple-key",
        "complex-key-123",
        "key-with-special-chars-!@#$%",
        "key with spaces",
        "key\nwith\nnewlines",
        "key\twith\ttabs",
        "UTF-8-key-こんにちは"
      })
  void testHashAndUnhashRoundTrip(String input) {
    String hashed = hashingService.hash(input);
    String unhashed = hashingService.unhash(hashed);

    assertThat(unhashed).isEqualTo(input);
    assertThat(hashed).isNotEqualTo(input);
  }

  @Test
  void testHashAndUnhashRoundTripWithLongKey() {
    String input = "very-long-key-" + "a".repeat(100);

    String hashed = hashingService.hash(input);
    String unhashed = hashingService.unhash(hashed);

    assertThat(unhashed).isEqualTo(input);
    assertThat(hashed).isNotEqualTo(input);
  }

  @Test
  void testHashProducesDifferentOutputForDifferentInputs() {
    String input1 = "key-1";
    String input2 = "key-2";

    String hash1 = hashingService.hash(input1);
    String hash2 = hashingService.hash(input2);

    assertThat(hash1).isNotEqualTo(hash2);
  }

  @Test
  void testHashProducesSameOutputForSameInput() {
    String input = "consistent-key";

    String hash1 = hashingService.hash(input);
    String hash2 = hashingService.hash(input);

    assertThat(hash1).isEqualTo(hash2);
  }

  @ParameterizedTest
  @NullAndEmptySource
  void testHashWithNullOrEmptyInput(String input) {
    String result = hashingService.hash(input);
    assertThat(result).isEqualTo(input);
  }

  @ParameterizedTest
  @NullAndEmptySource
  void testUnhashWithNullOrEmptyInput(String input) {
    String result = hashingService.unhash(input);
    assertThat(result).isEqualTo(input);
  }

  @Test
  void testUnhashWithInvalidBase64ReturnsOriginal() {
    String invalidBase64 = "not-valid-base64!!!";
    String result = hashingService.unhash(invalidBase64);

    assertThat(result).isEqualTo(invalidBase64);
  }

  @ParameterizedTest
  @MethodSource("provideHashedAndUnhashedPairs")
  void testKnownHashedValues(String plain, String expectedHash) {
    String hashed = hashingService.hash(plain);
    assertThat(hashed).isEqualTo(expectedHash);

    String unhashed = hashingService.unhash(expectedHash);
    assertThat(unhashed).isEqualTo(plain);
  }

  private static Stream<Arguments> provideHashedAndUnhashedPairs() {
    return Stream.of(
        Arguments.of("test", "dGVzdA=="),
        Arguments.of("hello", "aGVsbG8="),
        Arguments.of("idempotency-key-123", "aWRlbXBvdGVuY3kta2V5LTEyMw=="),
        Arguments.of("key", "a2V5"));
  }

  @Test
  void testHashIsReversible() {
    String original = "reversible-key";

    String hashed = hashingService.hash(original);
    String unhashed = hashingService.unhash(hashed);

    assertThat(unhashed).isEqualTo(original);
  }

  @Test
  void testMultipleRoundTrips() {
    String original = "multi-round-trip-key";

    String hashed1 = hashingService.hash(original);
    String unhashed1 = hashingService.unhash(hashed1);
    String hashed2 = hashingService.hash(unhashed1);
    String unhashed2 = hashingService.unhash(hashed2);

    assertThat(unhashed1).isEqualTo(original);
    assertThat(unhashed2).isEqualTo(original);
    assertThat(hashed1).isEqualTo(hashed2);
  }

  @Test
  void testHashedValueIsBase64() {
    String input = "base64-test";
    String hashed = hashingService.hash(input);

    assertThat(hashed).matches("^[A-Za-z0-9+/]*={0,2}$");
  }

  @Test
  void testUnhashWithPartiallyInvalidBase64() {
    String partiallyInvalid = "YWJj!!!";
    String result = hashingService.unhash(partiallyInvalid);

    assertThat(result).isEqualTo(partiallyInvalid);
  }
}
