package org.proxiadsee.interview.task.payment.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.stereotype.Service;

@Service
public class IdempotencyKeyHashingService {

  public String hash(String plainKey) {
    if (plainKey == null || plainKey.isEmpty()) {
      return plainKey;
    }
    return Base64.getEncoder().encodeToString(plainKey.getBytes(StandardCharsets.UTF_8));
  }

  public String unhash(String hashedKey) {
    if (hashedKey == null || hashedKey.isEmpty()) {
      return hashedKey;
    }
    try {
      byte[] decodedBytes = Base64.getDecoder().decode(hashedKey);
      return new String(decodedBytes, StandardCharsets.UTF_8);
    } catch (IllegalArgumentException ex) {
      return hashedKey;
    }
  }
}
