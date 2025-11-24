# Payment Service

A gRPC-based payment service built with Spring Boot, providing secure and reliable payment processing with idempotency support.

## 📋 Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Building](#building)
- [Running](#running)
- [Testing](#testing)
- [Calling the Service](#calling-the-service)
- [Docker](#docker)
- [Development](#development)
- [CI/CD](#cicd)
- [Security](#security)
- [Monitoring](#monitoring)

---

## Overview

The Payment Service a gRPC service that handles payment processing with the following features:

- ✅ **Idempotency** - Prevents duplicate payments with idempotency keys
- ✅ **Graceful Shutdown** - Zero-downtime deployments
- ✅ **Health Checks** - Container and application health monitoring
- ✅ **Metrics** - Prometheus-compatible metrics
- ✅ **Security** - Input validation, non-root containers, CVE scanning
- ✅ **Database** - SQLite for development, H2 for testing
- ✅ **Code Quality** - Checkstyle, SpotBugs, Spotless formatting

### Database Limitations

**⚠️ Important:** This is a test/demonstration project with the following database limitations:

- **SQLite (Development):** File-based database (`payment.db`). Data persists between restarts but:
  - Not suitable for production (no concurrent writes)
  - No built-in replication or high availability
  - Limited transaction isolation
  - Single-threaded write operations
  
- **H2 (Testing):** In-memory database. Data is lost when tests complete.

For production use, replace SQLite with a proper RDBMS (PostgreSQL, MySQL, etc.).

---

## Tech Stack

- **Java 21** - Latest LTS version
- **Spring Boot 3.5.7** - Application framework
- **gRPC** - Remote procedure calls
- **Spring Data JPA** - Database access
- **SQLite** - Development database
- **H2** - Test database
- **Flyway** - Database migrations
- **Lombok** - Boilerplate reduction
- **MapStruct** - Object mapping
- **JaCoCo** - Code coverage
- **Maven** - Build tool

---

## Prerequisites

- **Java 21 or higher** - [Download](https://adoptium.net/)
- **Maven** (optional - wrapper included)
- **Docker** (optional - for containerized deployment)

---

## Quick Start

### One-Command Run

**Windows (PowerShell):**
```powershell
cd cmd\server
.\run.ps1
```

**Linux/Mac:**
```bash
cd cmd/server
chmod +x run.sh
./run.sh
```

> **📝 Note:** The run script automatically creates `src/main/resources/env.properties` if it doesn't exist. You don't need to manually configure the database - default SQLite settings will be used.

The script will:
1. ✅ Check Java 21+ is installed
2. ✅ Create `env.properties` if missing (with default SQLite configuration)
3. ✅ Build the application
4. ✅ Start the service

**Access:**
- gRPC Service: `localhost:9090`
- Health Check: http://localhost:8081/actuator/health
- Metrics: http://localhost:8081/actuator/metrics

> **⚠️ Database Note:** This service uses SQLite (file-based, not in-memory) for development. Data persists in `payment.db` file. To start fresh, delete the `payment.db` file. For true in-memory database, H2 is used in tests only.

---

## Building

### Build JAR

```bash
# Clean build
./mvnw clean package

# Skip tests
./mvnw clean package -DskipTests

# With coverage report
./mvnw clean verify
```

### Build Docker Image

```bash
docker build -t payment-service:latest .
```

The Dockerfile uses:
- ✅ Multi-stage build (builder + runtime)
- ✅ JRE-based final image (~200-300 MB)
- ✅ Non-root user
- ✅ Health check
- ✅ Optimized JVM settings

---

## Running

### Local (JAR)

**Manual run:**
```bash
# Build first
./mvnw clean package -DskipTests

# Run
java -jar target/payment-service-*.jar
```

**With custom configuration:**
```bash
java -jar target/payment-service-*.jar \
  --spring.datasource.url=jdbc:sqlite:custom.db \
  --logging.level.root=DEBUG
```

### Docker

**Run container:**
```bash
docker run -d \
  --name payment-service \
  -p 9090:9090 \
  -p 8081:8081 \
  payment-service:latest
```

**Check health:**
```bash
docker ps  # Should show (healthy)
curl http://localhost:8081/actuator/health
```

**View logs:**
```bash
docker logs -f payment-service
```

**Stop (gracefully):**
```bash
docker stop payment-service  # Waits up to 30 seconds for graceful shutdown
```

### Docker Compose

```bash
docker-compose up -d
docker-compose logs -f
docker-compose down
```

---

## Testing

### Run All Tests

```bash
./mvnw test
```

### Run Specific Test

```bash
./mvnw test -Dtest=PaymentServiceTest
./mvnw test -Dtest=PaymentServiceTest#testRequestPayment
```

### Run with Coverage

```bash
./mvnw clean test jacoco:report
```

**View coverage report:**
```bash
# Linux/Mac
open target/site/jacoco/index.html

# Windows
start target/site/jacoco/index.html
```

### Integration Tests

```bash
./mvnw verify
```

### Coverage Gate

The project requires **80% minimum coverage**. Build fails if coverage is below threshold.

```bash
./mvnw clean test jacoco:report jacoco:check
```

---

## Calling the Service

### Using grpcurl

**Install grpcurl:**
```bash
# Mac
brew install grpcurl

# Linux
go install github.com/fullstorydev/grpcurl/cmd/grpcurl@latest

# Windows
choco install grpcurl
```

### Health Check

```bash
grpcurl -plaintext localhost:9090 grpc.health.v1.Health/Check
```

### Request Payment

```bash
grpcurl -plaintext \
  -d '{
    "amount_minor": 1000,
    "currency": "USD",
    "order_id": "order-123",
    "idempotency_key": "idem-key-001",
    "metadata": {
      "customer_id": "cust-456"
    }
  }' \
  localhost:9090 \
  payments.v1.PaymentService/RequestPayment
```

**Response:**
```json
{
  "payment_id": "1",
  "status": "PAYMENT_STATUS_SUCCEEDED",
  "message": "stub",
  "idempotency_key": "idem-key-001",
  "created_at": {
    "seconds": "1700000000",
    "nanos": 123456000
  }
}
```

### Get Payment

```bash
grpcurl -plaintext \
  -d '{"payment_id": "1"}' \
  localhost:9090 \
  payments.v1.PaymentService/GetPayment
```

### Idempotency Test

**First request:**
```bash
grpcurl -plaintext \
  -d '{
    "amount_minor": 5000,
    "currency": "EUR",
    "order_id": "order-456",
    "idempotency_key": "same-key-123",
    "metadata": {}
  }' \
  localhost:9090 \
  payments.v1.PaymentService/RequestPayment
```

**Second request (same idempotency key):**
```bash
# Returns the SAME payment (not a new one)
grpcurl -plaintext \
  -d '{
    "amount_minor": 5000,
    "currency": "EUR",
    "order_id": "order-456",
    "idempotency_key": "same-key-123",
    "metadata": {}
  }' \
  localhost:9090 \
  payments.v1.PaymentService/RequestPayment
```

**Third request (same key, different data):**
```bash
# Returns HTTP 409 CONFLICT (hash mismatch)
grpcurl -plaintext \
  -d '{
    "amount_minor": 9999,
    "currency": "EUR",
    "order_id": "order-456",
    "idempotency_key": "same-key-123",
    "metadata": {}
  }' \
  localhost:9090 \
  payments.v1.PaymentService/RequestPayment
```

### Using BloomRPC / Postman

1. Import the proto file: `src/main/proto/payment.proto`
2. Connect to: `localhost:9090`
3. Call methods with JSON payloads

---

## Docker

### Build

```bash
docker build -t payment-service:latest .
```

### Run

```bash
docker run -d \
  --name payment-service \
  -p 9090:9090 \
  -p 8081:8081 \
  -v $(pwd)/payment.db:/app/payment.db \
  payment-service:latest
```

### Docker Compose

```yaml
version: '3.8'
services:
  payment-service:
    build: .
    ports:
      - "9090:9090"
      - "8081:8081"
    volumes:
      - ./payment.db:/app/payment.db
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8081/actuator/health"]
      interval: 30s
      timeout: 3s
      retries: 3
      start_period: 60s
```

### Security Scan

```bash
# Build image
docker build -t payment-service:latest .

# Scan with Trivy
docker run --rm -v /var/run/docker.sock:/var/run/docker.sock \
  aquasec/trivy image payment-service:latest
```

---

## Development

### Code Formatting

```bash
# Check format
./mvnw spotless:check

# Auto-format
./mvnw spotless:apply
```

### Code Quality Checks

```bash
# Checkstyle
./mvnw checkstyle:check

# SpotBugs
./mvnw spotbugs:check

# All checks
./mvnw clean verify
```

### Database Migrations

**Create new migration:**
1. Add file: `src/main/resources/db/migration/V3__description.sql`
2. Write SQL DDL/DML
3. Restart app (Flyway auto-applies)

**Check migration status:**
```bash
./mvnw flyway:info
```

### Project Structure

```
payment-service/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── org/proxiadsee/interview/task/payment/
│   │   │       ├── config/          # Spring configuration
│   │   │       ├── domain/
│   │   │       │   ├── dto/         # Data Transfer Objects
│   │   │       │   └── entity/      # JPA entities
│   │   │       ├── exception/       # Custom exceptions & handlers
│   │   │       ├── mapper/          # MapStruct mappers
│   │   │       ├── service/         # Business logic
│   │   │       ├── storage/         # Repositories
│   │   │       └── validation/      # Input validation
│   │   ├── proto/
│   │   │   └── payment.proto        # gRPC service definition
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── env.properties       # Database config (gitignored)
│   │       └── db/migration/        # Flyway migrations
│   └── test/
│       └── java/                    # Unit & integration tests
├── cmd/
│   └── server/
│       ├── run.sh                   # One-command run (Linux/Mac)
│       └── run.ps1                  # One-command run (Windows)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Local docker setup
└── pom.xml                          # Maven build file
```

---

## CI/CD

### GitHub Actions

The CI pipeline runs on every push and PR:

```
Lint (Checkstyle, SpotBugs, Spotless)
  ↓
  ├─→ Dependency Scan (OWASP, Trivy) [parallel]
  └─→ Test + Coverage                [parallel]
        ↓
      Build Docker + Health Check
```

**Features:**
- ✅ Fail-fast strategy
- ✅ Dependency caching (90% faster)
- ✅ Security scanning (CVE detection)
- ✅ Code coverage gate (10% minimum)
- ✅ Docker build & health verification

**Run locally:**
```bash
# Requires 'act' tool
act -j lint
act -j test
act -j build
```

---

## Security

### Dependency Scanning

**Run OWASP Dependency Check:**
```bash
./mvnw org.owasp:dependency-check-maven:10.0.4:check
open target/dependency-check-report.html
```

**Scan Docker image:**
```bash
docker build -t payment-service:latest .
docker run --rm -v /var/run/docker.sock:/var/run/docker.sock \
  aquasec/trivy image payment-service:latest
```

### Security Features

- ✅ **Input Validation** - Bean Validation on all DTOs
- ✅ **SQL Injection Protection** - JPA parameterized queries
- ✅ **No Sensitive Logging** - Idempotency keys not logged
- ✅ **Non-Root Container** - Runs as `appuser`
- ✅ **Graceful Shutdown** - No dropped requests
- ✅ **Exception Sanitization** - No stack traces to clients
- ✅ **Idempotency Key Hashing** - Base64 encoding (upgradable to SHA-256)

---

## Monitoring

### Health Check

```bash
curl http://localhost:8081/actuator/health
```

**Response:**
```json
{
  "status": "UP"
}
```

### Metrics

**All metrics:**
```bash
curl http://localhost:8081/actuator/metrics
```

**Specific metric:**
```bash
curl http://localhost:8081/actuator/metrics/jvm.memory.used
curl http://localhost:8081/actuator/metrics/hikaricp.connections.active
```

### Prometheus

**Scrape endpoint:**
```
http://localhost:8081/actuator/prometheus
```

**Add to prometheus.yml:**
```yaml
scrape_configs:
  - job_name: 'payment-service'
    static_configs:
      - targets: ['localhost:8081']
    metrics_path: '/actuator/prometheus'
```

### Available Metrics

- JVM memory, threads, GC
- HikariCP connection pool
- Application startup time
- HTTP request metrics
- Database query metrics
- Custom business metrics (if added)

---

## API Reference

### gRPC Service Definition

See: `src/main/proto/payment.proto`

### Endpoints

#### RequestPayment
Request a new payment with idempotency support.

**Request:**
```protobuf
message RequestPaymentRequest {
  int64 amount_minor = 1;           // Amount in smallest currency unit (e.g., cents)
  string currency = 2;              // 3-letter ISO currency code (e.g., USD)
  string order_id = 3;              // Merchant order ID
  string idempotency_key = 4;       // Unique key for idempotency
  map<string, string> metadata = 5; // Optional key-value metadata
}
```

**Response:**
```protobuf
message RequestPaymentResponse {
  string payment_id = 1;            // Unique payment ID
  PaymentStatus status = 2;         // Payment status
  string message = 3;               // Status message
  string idempotency_key = 4;       // Echo of idempotency key
  google.protobuf.Timestamp created_at = 5;
}
```

#### GetPayment
Retrieve payment details by ID.

**Request:**
```protobuf
message GetPaymentRequest {
  string payment_id = 1;            // Payment ID to retrieve
}
```

**Response:**
```protobuf
message GetPaymentResponse {
  string payment_id = 1;
  int64 amount_minor = 2;
  string currency = 3;
  string order_id = 4;
  PaymentStatus status = 5;
  string message = 6;
  string idempotency_key = 7;
  google.protobuf.Timestamp created_at = 8;
  map<string, string> metadata = 9;
}
```

#### Health
Standard gRPC health check.

**Request:**
```protobuf
message HealthRequest {}
```

**Response:**
```protobuf
message HealthResponse {
  string status = 1;  // "OK"
}
```

---

## Configuration

### Environment Variables

Create `src/main/resources/env.properties`:

```properties
db.url=jdbc:sqlite:payment.db
db.username=
db.password=
```

### Application Properties

Key settings in `application.properties`:

```properties
# Server
spring.grpc.server.port=9090
management.server.port=8081

# Graceful Shutdown
server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=30s

# Database
spring.datasource.url=${db.url}
spring.jpa.hibernate.ddl-auto=none
spring.flyway.enabled=true

# Logging
logging.level.root=INFO
logging.level.org.proxiadsee.interview.task.payment=DEBUG
```

---

## Troubleshooting

### Port Already in Use

**Linux/Mac:**
```bash
lsof -ti:9090 | xargs kill
lsof -ti:8081 | xargs kill
```

**Windows:**
```powershell
netstat -ano | findstr :9090
taskkill /PID <PID> /F
```

### Build Fails

```bash
# Clean everything
./mvnw clean
rm -rf target/

# Rebuild
./mvnw clean package
```

### Database Locked

```bash
# Stop all instances
pkill -f payment-service

# Remove database
rm payment.db

# Restart (database auto-created)
```

### Container Unhealthy

```bash
# Check logs
docker logs payment-service

# Check health endpoint
curl http://localhost:8081/actuator/health

# Restart
docker restart payment-service
```

---

## Architecture

For detailed architecture documentation including:
- Data model and database schema
- Idempotency strategy and implementation
- Error handling model
- Design patterns used
- Security considerations and limitations
- Directory structure
- Technology choices

See: **[ARCHITECTURE.md](ARCHITECTURE.md)**

---

## Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Run tests and checks (`./mvnw clean verify`)
4. Format code (`./mvnw spotless:apply`)
5. Commit changes (`git commit -m 'Add amazing feature'`)
6. Push to branch (`git push origin feature/amazing-feature`)
7. Open Pull Request

**Code quality requirements:**
- ✅ All tests pass
- ✅ Coverage ≥ 10%
- ✅ No Checkstyle violations
- ✅ No SpotBugs warnings
- ✅ Code formatted with Spotless

---

## License

[Add your license here]

---

## Support

For issues, questions, or contributions, please open an issue on GitHub.

---
