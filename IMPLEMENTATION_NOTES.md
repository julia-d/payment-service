# Implementation Notes

## 1. How the project was built

- The payment-service codebase was fully developed with AI support.
- All main components (services, repositories, mappers, tests, CI, Docker, docs) were generated or strongly assisted by AI.
- The overall architecture and boundaries were defined and guided by a human:
  - Package layout and layering
  - Idempotency strategy
  - Error model and gRPC mapping
  - CI/CD expectations and quality gates
  - Simplification and scope decisions

---

## 2. Experience level and learning goals

- At the start of this project, I was new to payment services as a domain.
- I was also new to gRPC in practice:
  - Learned how to define `.proto` files
  - Learned how to wire Spring Boot with gRPC
  - Learned how to test gRPC flows from a client point of view
  - Learned how to set up actuator metrics for a gRPC server; before this project I only used actuator with a Spring Boot web application
- I did not have practical experience implementing GitHub CI pipelines by myself.
  - I had previous experience working on a project that already used GitHub CI
  - I made changes to existing pipelines, but did not design them from scratch before this project

---

## 3. Tech stack and structure

- The project uses an up-to-date tech stack:
  - Java 21
  - Spring Boot 3.x
  - gRPC
  - Spring Data JPA
  - Flyway
  - SQLite (dev) and H2 (tests)
  - Lombok and MapStruct
  - JaCoCo, Checkstyle, SpotBugs, Spotless
- The codebase preserves standard Java + Spring structure:
  - Clear separation into `config`, `domain`, `service`, `storage`, `validation`, `exception`, `mapper`
  - Resources under `src/main/resources` with `application.properties`, Flyway migrations, env config
  - Tests under `src/test/java` following the same package layout

---

## 4. Simplified parts and why

Some parts of the system use deliberately simplified implementations. This is by design, to keep the project focused and time-efficient as a showcase.

### 4.2 Idempotency key hashing and storage

- Idempotency keys and request hashes are stored in a simple table.
- The hashing strategy is minimal and not cryptographically strong:
  - It is just enough to detect "same request vs different request" for the demo
  - It is not meant as security or tamper-proof mechanism
- Reason:
  - The point is to demonstrate idempotency flow and conflict detection, not to implement a hardened, production-grade hashing and storage layer.

### 4.3 Payment gateway stub

- The integration with a real external payment gateway is stubbed:
  - There is a `PaymentGatewayService` that returns a fixed stub value
  - No external network calls, no real provider integration
- Reason:
  - Focus on internal idempotency, storage, validation, status handling
  - Keep the project self-contained, fast to run, and stable for tests

In short, these simplified parts are there to show the necessity and place of these features, while using the most time-efficient approach for a showcase project.

---

## 5. Missing gRPC security and why

The project does not implement gRPC security features such as:
- TLS / mTLS on the gRPC channel
- Authentication or authorization on RPC calls
- API keys, JWT, OAuth2, or similar mechanisms
- Rate limiting or abuse protection on gRPC endpoints

Security for gRPC and external access was explicitly out of scope for this project.

The focus of this implementation is:
- Payment flow logic
- Idempotency behavior
- Validation rules
- Persistence and schema evolution
- CI/CD pipeline and runtime packaging

In a real payment system, these security features are mandatory and must be added. Here they are intentionally left out to keep the project small, focused, and easy to read.
