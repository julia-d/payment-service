# Architecture Documentation - Payment Service

## Overview

This is a **test/demonstration project** implementing a gRPC-based payment service with idempotency support. The architecture follows standard Spring Boot patterns with some simplifications appropriate for a demonstration project.

---

## System Architecture

### High-Level Design

```
┌─────────────┐
│   Client    │
│  (gRPC)     │
└──────┬──────┘
       │
       ▼
┌──────────────────────────────────────────┐
│         gRPC Server Layer                │
│  - PaymentService (gRPC endpoint)        │
│  - GrpcExceptionAdvice (error handling)  │
└──────┬───────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────┐
│         Service Layer                    │
│  - PaymentService (orchestration)        │
│  - ProcessPaymentService (business logic)│
│  - PaymentGatewayService (stub)          │
│  - IdempotencyKeyHashingService          │
└──────┬───────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────┐
│         Data Layer                       │
│  - PaymentRepository                     │
│  - IdempotencyKeyRepository              │
└──────┬───────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────┐
│         Database                         │
│  - SQLite (development)                  │
│  - H2 (testing)                          │
└──────────────────────────────────────────┘
```

---

## Data Model

### Database Schema

#### Table: `idempotency_key`
Stores idempotency keys to prevent duplicate payments.

```sql
CREATE TABLE idempotency_key (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  key_value TEXT NOT NULL UNIQUE,          -- Hashed idempotency key
  request_hash TEXT NOT NULL,              -- Hash of entire request for validation
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_idempotency_key_value ON idempotency_key(key_value);
```

**Fields:**
- `id`: Auto-increment primary key
- `key_value`: Base64-encoded idempotency key (hashed for storage)
- `request_hash`: Hash of request payload (using Java `hashCode()`)
- `created_at`: Timestamp for auditing

#### Table: `payment`
Stores payment records.

```sql
CREATE TABLE payment (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  idempotency_id INTEGER NOT NULL UNIQUE,  -- FK to idempotency_key (one-to-one)
  gateway_payment_id TEXT UNIQUE,          -- External payment gateway ID
  amount_minor INTEGER NOT NULL,           -- Amount in smallest currency unit
  currency TEXT NOT NULL,                  -- ISO 4217 currency code
  status TEXT NOT NULL,                    -- Payment status
  order_id TEXT NOT NULL,                  -- Merchant order ID
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  metadata TEXT,                           -- JSON metadata
  message TEXT,                            -- Status message
  FOREIGN KEY (idempotency_id) REFERENCES idempotency_key(id)
);

-- Indexes
CREATE UNIQUE INDEX idx_payment_idempotency_id ON payment(idempotency_id);
CREATE INDEX idx_payment_gateway_id ON payment(gateway_payment_id);
CREATE INDEX idx_payment_order_id ON payment(order_id);
CREATE INDEX idx_payment_created_at ON payment(created_at);
```

**Relationships:**
- One-to-one relationship between `payment` and `idempotency_key`
- `idempotency_id` is unique to ensure one payment per idempotency key

### Entity Model

```
┌────────────────────────┐
│ IdempotencyKeyEntity   │
│────────────────────────│
│ - id: Long             │
│ - value: String        │ (hashed)
│ - requestHash: String  │
│ - createdAt: LocalDateTime │
└────────────────────────┘
           │ 1
           │
           │ 1
┌────────────────────────┐
│   PaymentEntity        │
│────────────────────────│
│ - id: Long             │
│ - idempotencyKeyId: IdempotencyKeyEntity │
│ - gatewayPaymentId: String │
│ - amountMinor: Long    │
│ - currency: String     │
│ - status: String       │
│ - orderId: String      │
│ - createdAt: LocalDateTime │
│ - metadata: String     │ (JSON)
│ - message: String      │
└────────────────────────┘
```

---

## Idempotency Strategy

### Design Goals

1. **Prevent duplicate payments** - Same idempotency key = same payment
2. **Detect request tampering** - Reject if payload changes for same key
3. **Support retries** - Allow clients to safely retry failed requests

### Implementation

#### Step 1: Idempotency Key Processing

```java
// Client sends idempotency key
String plainKey = request.getIdempotencyKey();

// Hash the key (Base64 encoding for simplicity)
String hashedKey = hashingService.hash(plainKey);

// Check if key exists
Optional<IdempotencyKeyEntity> existing = 
    idempotencyKeyRepository.findByValue(hashedKey);
```

#### Step 2: Request Hash Validation

```java
// Calculate hash of current request
String currentHash = String.valueOf(dto.hashCode());

if (existingKey) {
    // Compare with stored hash
    if (!currentHash.equals(storedHash)) {
        throw ConflictException("Request data mismatch");
    }
    // Return existing payment
    return existingPayment;
}
```

#### Step 3: New Payment Processing

```java
@Transactional(rollbackFor = Exception.class)
public RequestPaymentResponse processNewPayment(...) {
    // Save idempotency key
    idempotencyKeyRepository.save(newKey);
    
    // Create payment
    PaymentEntity payment = createPayment(dto, newKey);
    paymentRepository.save(payment);
    
    // Process with gateway (stub)
    GatewayResponse response = gateway.process(dto);
    
    // Update payment with gateway result
    payment.setGatewayPaymentId(response.id());
    payment.setStatus(response.status());
    paymentRepository.save(payment);
    
    return toResponse(payment);
}
```

**Rollback on Failure:**
- If any step fails, transaction rolls back
- Idempotency key is deleted
- Client can retry with same key

### Idempotency Flow Diagram

```
┌─────────────┐
│ New Request │
└──────┬──────┘
       │
       ▼
┌──────────────────────────┐
│ Hash Idempotency Key     │
└──────┬───────────────────┘
       │
       ▼
    ┌──────────────┐
    │ Key Exists?  │
    └──┬────────┬──┘
       │ Yes    │ No
       ▼        ▼
┌──────────┐  ┌──────────────┐
│Get Payment│  │Create New Key│
│& Check   │  │Save to DB    │
│Hash Match│  └──────┬───────┘
└──┬───────┘         │
   │                 ▼
   │          ┌──────────────┐
   │          │Create Payment│
   │          │Process       │
   │          └──────┬───────┘
   │                 │
   ├─────────────────┘
   │
   ▼
┌──────────────┐
│Return Payment│
└──────────────┘
```

### Limitations (Test Project Simplifications)

⚠️ **This is a simplified implementation for demonstration purposes:**

1. **Hashing Algorithm:**
   - Uses Base64 encoding (not cryptographic hashing)
   - Production: Use SHA-256 or similar
   - Current: Easily reversible (not secure)

2. **Request Hash:**
   - Uses Java `hashCode()` (weak, collision-prone)
   - Production: Use cryptographic hash (SHA-256) of request JSON
   - Current: Hash collisions possible

3. **No Cleanup:**
   - Idempotency keys stored forever
   - Production: TTL/cleanup job needed
   - Current: Database grows unbounded

4. **Database:**
   - SQLite (single-threaded writes)
   - Production: PostgreSQL/MySQL with proper locking
   - Current: Race conditions possible under high load

---

## Error Model

### Error Handling Strategy

The service uses gRPC status codes for error communication:

```
┌─────────────────┐
│   Exception     │
└────────┬────────┘
         │
    ┌────┴────────────────────────────┐
    │                                 │
    ▼                                 ▼
┌──────────────┐              ┌──────────────┐
│ Business     │              │  System      │
│ Exceptions   │              │  Exceptions  │
└──────┬───────┘              └──────┬───────┘
       │                             │
       ├─ ValidationException        ├─ ServiceException
       ├─ ConflictException          └─ Unexpected errors
       └─ ConstraintViolationException
       │
       ▼
┌──────────────────────┐
│ gRPC Status Mapping  │
├──────────────────────┤
│ INVALID_ARGUMENT     │ ← ValidationException, ConflictException
│ INTERNAL             │ ← ServiceException, Unexpected
│ NOT_FOUND            │ ← (not implemented)
└──────────────────────┘
```

### Exception Types

#### ValidationException
**Trigger:** Invalid input data (currency, amount, etc.)
**gRPC Status:** `INVALID_ARGUMENT`
**Response:** Error message describing validation failure

#### ConflictException
**Trigger:** Idempotency key reused with different data
**gRPC Status:** `INVALID_ARGUMENT`
**Response:** "Request hash does not match stored hash"

#### ServiceException
**Trigger:** Internal processing errors
**gRPC Status:** `INTERNAL`
**Response:** "Internal server error" (no details leaked)

#### ConstraintViolationException
**Trigger:** Bean validation failures
**gRPC Status:** `INVALID_ARGUMENT`
**Response:** Validation constraint message

### Error Response Flow

```java
try {
    // Process request
} catch (ConflictException ex) {
    // Client error - safe to expose message
    Status.INVALID_ARGUMENT.withDescription(ex.getMessage())
} catch (ValidationException ex) {
    // Client error - safe to expose message
    Status.INVALID_ARGUMENT.withDescription(ex.getMessage())
} catch (ServiceException ex) {
    // Server error - hide details
    Status.INTERNAL.withDescription("Internal server error")
    // Log full stack trace server-side
}
```

**Security Note:** Stack traces and internal details are **never** sent to clients.

---

## Design Patterns

### 1. Service Layer Pattern
**Purpose:** Separate business logic from gRPC implementation

```
PaymentService (gRPC) → ProcessPaymentService (Business Logic)
                      → PaymentGatewayService (External calls)
```

### 2. Repository Pattern
**Purpose:** Abstract data access

```
PaymentRepository ← Spring Data JPA
IdempotencyKeyRepository ← Spring Data JPA
```

### 3. DTO Pattern
**Purpose:** Decouple gRPC models from domain entities

```
gRPC Request → RequestPaymentRequestDTO → PaymentEntity
PaymentEntity → GetPaymentResponse (via MapStruct)
```

### 4. Mapper Pattern (MapStruct)
**Purpose:** Type-safe object mapping

```java
@Mapper(componentModel = "spring")
public abstract class PaymentMapper {
    abstract RequestPaymentRequestDTO toDto(RequestPaymentRequest request);
    abstract GetPaymentResponse toGetPaymentResponse(PaymentEntity entity);
}
```

### 5. Exception Interceptor Pattern
**Purpose:** Centralized error handling

```java
@Component
public class GrpcExceptionAdvice implements ServerInterceptor {
    // Intercepts all exceptions
    // Maps to appropriate gRPC status codes
}
```

### 6. Transaction Management
**Purpose:** Ensure data consistency

```java
@Transactional(rollbackFor = Exception.class)
public RequestPaymentResponse processNewPayment(...) {
    // All-or-nothing: success or full rollback
}
```

---

## Security Considerations

### ⚠️ Out of Scope (Test Project)

The following security features are **NOT implemented** as this is a test/demonstration project:

#### 1. Authentication & Authorization
- **Missing:** No client authentication
- **Missing:** No API keys or JWT tokens
- **Missing:** No role-based access control (RBAC)
- **Production:** Implement mTLS or OAuth 2.0

#### 2. Rate Limiting
- **Missing:** No request throttling
- **Missing:** No DDoS protection
- **Missing:** No per-client quotas
- **Production:** Implement rate limiting (e.g., Resilience4j)

#### 3. Encryption
- **Missing:** No TLS/SSL for gRPC
- **Missing:** Data transmitted in plaintext
- **Production:** Enable gRPC TLS with certificates

#### 4. Advanced Idempotency Security
- **Current:** Simple Base64 encoding
- **Missing:** Cryptographic hashing (SHA-256)
- **Missing:** HMAC for tamper detection
- **Production:** Use proper cryptographic functions

#### 5. Audit Logging
- **Missing:** No audit trail for payments
- **Missing:** No change tracking
- **Production:** Implement comprehensive audit logs

#### 6. Input Sanitization
- **Current:** Basic validation only
- **Missing:** Advanced SQL injection protection
- **Missing:** XSS protection for metadata
- **Production:** Comprehensive input sanitization

### ✅ Implemented Security Features

1. **Input Validation:** Bean Validation on all DTOs
2. **SQL Injection Protection:** JPA parameterized queries
3. **Non-Root Container:** Docker runs as `appuser`
4. **Graceful Shutdown:** Prevents dropped requests
5. **Exception Sanitization:** No stack traces to clients
6. **Dependency Scanning:** OWASP & Trivy in CI/CD

---

## Directory Structure

The project follows **standard Java/Maven structure**:

```
payment-service/
│
├── src/main/java/                         # Application source code
│   └── org/proxiadsee/interview/task/payment/
│       ├── PaymentServiceApplication.java # Spring Boot main class
│       ├── config/                        # Configuration classes
│       │   ├── GrpcServerConfig.java
│       │   ├── MetricsConfig.java
│       │   └── SQLiteDialect.java
│       ├── domain/                        # Domain models
│       │   ├── dto/                       # Data Transfer Objects
│       │   │   ├── GetPaymentRequestDTO.java
│       │   │   ├── RequestPaymentRequestDTO.java
│       │   │   ├── GatewayPaymentDTO.java
│       │   │   └── PaymentStatusDTO.java
│       │   └── entity/                    # JPA Entities
│       │       ├── IdempotencyKeyEntity.java
│       │       └── PaymentEntity.java
│       ├── exception/                     # Custom exceptions
│       │   ├── ConflictException.java
│       │   ├── ServiceException.java
│       │   ├── ValidationException.java
│       │   └── GrpcExceptionAdvice.java   # Global exception handler
│       ├── mapper/                        # MapStruct mappers
│       │   └── PaymentMapper.java
│       ├── service/                       # Business logic
│       │   ├── PaymentService.java        # gRPC service implementation
│       │   ├── ProcessPaymentService.java # Payment processing logic
│       │   ├── PaymentGatewayService.java # Gateway stub
│       │   └── IdempotencyKeyHashingService.java
│       ├── storage/                       # Data access layer
│       │   ├── IdempotencyKeyRepository.java
│       │   └── PaymentRepository.java
│       └── validation/                    # Custom validators
│           └── DtoValidator.java
│
├── src/main/proto/                        # gRPC protocol definitions
│   └── payment.proto
│
├── src/main/resources/                    # Application resources
│   ├── application.properties             # Main config
│   ├── env.properties                     # Database config (gitignored)
│   └── db/migration/                      # Flyway migrations
│       ├── V1__create_idempotency_key_table.sql
│       └── V2__add_indexes.sql
│
├── src/test/java/                         # Test source code
│   └── org/proxiadsee/interview/task/payment/
│       ├── it/                            # Integration tests
│       │   └── PaymentClientFlowIntegrationTest.java
│       ├── service/                       # Service tests
│       │   ├── PaymentServiceTest.java
│       │   ├── ProcessPaymentServiceTest.java
│       │   └── ProcessPaymentServiceIntegrationTest.java
│       ├── storage/                       # Repository tests
│       │   ├── IdempotencyKeyRepositoryTest.java
│       │   └── PaymentRepositoryTest.java
│       └── validation/                    # Validation tests
│           └── DtoValidatorTest.java
│
├── src/test/resources/                    # Test resources
│   └── application-test.properties        # Test configuration
│
├── cmd/server/                            # One-command run scripts
│   ├── run.sh                             # Linux/Mac runner
│   ├── run.ps1                            # Windows runner
│   └── README.md
│
├── .github/workflows/                     # CI/CD pipelines
│   └── ci.yml                             # GitHub Actions workflow
│
├── target/                                # Build output (gitignored)
│   ├── classes/                           # Compiled classes
│   ├── generated-sources/                 # Generated code (gRPC, MapStruct)
│   └── payment-service-*.jar              # Executable JAR
│
├── Dockerfile                             # Multi-stage Docker build
├── docker-compose.yml                     # Local Docker setup
├── pom.xml                                # Maven build configuration
├── README.md                              # User documentation
├── ARCHITECTURE.md                        # This file
└── payment.db                             # SQLite database (gitignored)
```

### Package Organization

```
org.proxiadsee.interview.task.payment
│
├── config/         # Spring configuration and custom beans
├── domain/         # Core domain models (entities, DTOs)
├── exception/      # Custom exceptions and error handling
├── mapper/         # Object mapping (MapStruct)
├── service/        # Business logic and orchestration
├── storage/        # Data access (repositories)
└── validation/     # Input validation logic
```

**Principle:** Package by feature/layer, following Spring Boot conventions.

---

## Technology Choices

### Why gRPC?
- ✅ Strongly-typed API contracts (Protocol Buffers)
- ✅ High performance (binary serialization)
- ✅ Built-in streaming support
- ✅ Language-agnostic (proto definitions)
- ✅ HTTP/2 multiplexing

### Why SQLite (Development)?
- ✅ Zero configuration
- ✅ File-based (easy to reset)
- ✅ Good for demos/testing
- ⚠️ **Not for production** (see Database Limitations)

### Why MapStruct?
- ✅ Compile-time code generation
- ✅ Type-safe mapping
- ✅ Better performance than reflection-based mappers
- ✅ IDE-friendly (errors at compile time)

### Why Flyway?
- ✅ Version-controlled migrations
- ✅ Repeatable deployments
- ✅ Automatic schema updates
- ✅ Rollback support

---

## Deployment Architecture

### Local Development
```
Developer Machine
  └─ Java 21 + Maven
       └─ SQLite (payment.db)
```

### Docker Deployment
```
Docker Container
  ├─ JRE 21 (eclipse-temurin)
  ├─ Non-root user (appuser)
  ├─ Health check (Actuator)
  └─ Volume: /app/payment.db
```

### Production Considerations (Not Implemented)

For production deployment, consider:

1. **Database:** Replace SQLite with PostgreSQL/MySQL
2. **High Availability:** Multiple instances behind load balancer
3. **Monitoring:** Prometheus + Grafana
4. **Logging:** Centralized logging (ELK stack)
5. **Secrets Management:** Vault or cloud secret manager
6. **TLS/SSL:** Enable gRPC TLS
7. **Authentication:** Implement OAuth 2.0 or mTLS
8. **Rate Limiting:** Add Resilience4j or similar
9. **Caching:** Redis for session/data caching
10. **Message Queue:** For async payment processing

---

## Performance Characteristics

### Database Performance

**SQLite Limitations:**
- Single writer at a time
- No connection pooling benefit
- File I/O bound
- **Throughput:** ~1,000 writes/sec (single thread)

**For Production:**
- Use PostgreSQL/MySQL
- Connection pooling (HikariCP)
- Read replicas for scaling
- **Throughput:** 10,000+ writes/sec (clustered)

### Memory Usage

- **JVM Heap:** Configured to 75% of container memory
- **Typical usage:** 256-512 MB
- **Startup time:** ~10-15 seconds
- **Request latency:** <10ms (without network I/O)

---

## Testing Strategy

### Test Pyramid

```
          ┌────────┐
          │  IT    │  Integration Tests (full Spring context)
          └────────┘
        ┌────────────┐
        │   Service  │  Service Layer Tests (mocked dependencies)
        └────────────┘
    ┌──────────────────┐
    │   Repository     │  Repository Tests (H2 database)
    └──────────────────┘
  ┌──────────────────────┐
  │     Validation       │  Unit Tests (pure logic)
  └──────────────────────┘
```

**Coverage Target:** 10% minimum (enforced by JaCoCo)

### Test Types

1. **Unit Tests:** Service logic with mocks
2. **Repository Tests:** H2 in-memory database
3. **Integration Tests:** Full Spring Boot context
4. **End-to-End Tests:** Full client flow simulation

---

## Future Enhancements

### Short Term
- [ ] Implement proper SHA-256 hashing for idempotency keys
- [ ] Add request/response logging (non-sensitive data)
- [ ] Implement idempotency key cleanup job (TTL)

### Medium Term
- [ ] Replace SQLite with PostgreSQL
- [ ] Add authentication (JWT or mTLS)
- [ ] Implement rate limiting
- [ ] Add distributed tracing (OpenTelemetry)

### Long Term
- [ ] Event sourcing for payment state
- [ ] Async payment processing with message queue
- [ ] Multi-region deployment
- [ ] Advanced fraud detection

---

## Conclusion

This architecture demonstrates a well-structured gRPC service with:

✅ Clear separation of concerns (layers)  
✅ Proper error handling  
✅ Basic idempotency support  
✅ Standard Java project structure  
✅ Production-ready CI/CD  

However, as a **test/demonstration project**, it intentionally omits:

⚠️ Production-grade database  
⚠️ Authentication & authorization  
⚠️ Rate limiting  
⚠️ Advanced security features  
⚠️ Cryptographic hashing  

For production deployment, these features must be implemented.

---

**Document Version:** 1.0  
**Last Updated:** November 20, 2025  
**Status:** Test/Demonstration Project

