package org.proxiadsee.interview.task.payment.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "payment")
@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne
  @JoinColumn(name = "idempotency_id", nullable = false, unique = true)
  private IdempotencyKeyEntity idempotencyKey;

  @Column(name = "gateway_payment_id", unique = true)
  private String gatewayPaymentId;

  @Column(name = "amount_minor", nullable = false)
  private Long amountMinor;

  @Column(nullable = false)
  private String currency;

  @Column(nullable = false)
  private String status;

  @Column(name = "order_id", nullable = false)
  private String orderId;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column private String metadata;

  @Column private String message;
}
