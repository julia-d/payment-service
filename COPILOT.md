# AI-Assisted Development Approach

## Overview

This payment-service project was fully developed with AI assistance. The codebase, tests, CI/CD pipeline, Docker setup, and documentation were all generated or strongly guided by AI tools under human architectural direction.

## AI Tools and Models Used

Based on the development timeline from November 18-24, 2025, the following AI tools were used:

- **GitHub Copilot (JetBrains IntelliJ IDEA)** - Primary AI assistant for code generation and problem-solving
- **Multiple AI Models** - The copilot system leveraged various models during the 4-day development period
  
  > **Note:** Mostly I tried to use Claude Sonnet 4.5. I also used GPT 5.1 models and other Claude models because Claude Sonnet has tokens limit.

- **Context-aware assistance** - Used copilot-instructions.md for consistent coding standards

## Development Approach

### 1. Human-Defined Architecture, AI-Generated Implementation

The approach followed a clear division of responsibility:

**Human responsibilities:**
- Overall system architecture and component boundaries
- Package structure and layering decisions
- Idempotency strategy selection
- Error handling model and gRPC status mapping
- CI/CD pipeline expectations and quality gates
- Scope boundaries and simplification decisions
- Technology stack selection (Java 21, Spring Boot, gRPC, SQLite, etc.)

**AI responsibilities:**
- Code generation for services, repositories, entities, DTOs
- MapStruct mapper implementations
- Test case generation (unit, integration, parameterized)
- Maven plugin configurations (Spotless, Checkstyle, SpotBugs, JaCoCo)
- GitHub Actions CI/CD workflow
- Dockerfile multi-stage build
- Flyway migration scripts
- Documentation (README, ARCHITECTURE, IMPLEMENTATION_NOTES)
- Troubleshooting and error resolution

### 2. Directive Prompting Style

The prompts used throughout the project were:

- **Direct and action-oriented** - "Add X", "Fix Y", "Update Z" rather than questions
- **Context-rich** - Referenced specific files, error messages, and configuration when relevant
- **Incremental** - Built complexity layer by layer rather than requesting everything at once
- **Error-driven** - When compilation or runtime errors occurred, provided full stack traces for AI to analyze
- **Standards-enforced** - Consistently referenced copilot-instructions.md for coding conventions

Examples of prompt patterns:
- "Create PaymentService with stub methods for payments and a method for health check"
- "Fix error: [full stack trace]"
- "Add validation annotations to DTO classes for the following requirements: [specific rules]"
- "Update package names to make them short after payment folder"
- "Exclude entities and DTOs from coverage. Exclude generated sources"

### 3. Iterative Refinement Through Feedback Loops

Development followed a tight feedback loop:

1. **Prompt** - Clear, specific instruction
2. **AI Generation** - Code or configuration generated
3. **Verification** - Compile, run tests, check errors
4. **Correction** - If issues found, provide error details back to AI
5. **Repeat** - Continue until feature works correctly

This pattern was applied to:
- JaCoCo coverage configuration (multiple iterations to get exclusions right)
- Flyway scripts (adjusted for SQLite vs H2 dialect differences)
- GitHub Actions workflow (fixed deprecated actions, SARIF uploads, secret handling)
- Docker health checks (evolved from manual polling to Docker-native HEALTHCHECK)
- Entity generation strategies (fixed Hibernate sequence issues with SQLite)

### 4. Configuration-Driven Code Quality

Rather than manually enforcing code standards, AI was instructed to:

- Configure Spotless with google-java-format
- Set up Checkstyle rules
- Enable SpotBugs for static analysis
- Configure JaCoCo with 10% coverage threshold
- Apply these tools in CI pipeline

The copilot-instructions.md file enforced:
- No `var` keyword, use explicit types
- No comments unless explicitly requested
- Java records for DTOs
- Lombok for entities
- Builder pattern with vertical method chaining
- Parameterized tests

### 5. Knowledge Gap Bridging

The AI was used to bridge knowledge gaps in areas where I had limited experience:

- **gRPC fundamentals** - How to define .proto files, wire Spring Boot with gRPC server, handle streaming
- **Payment service domain** - Idempotency patterns, payment status flows, gateway integration
- **GitHub Actions CI** - Pipeline structure, caching strategies, secret handling, SARIF uploads
- **Actuator with gRPC** - Setting up metrics for gRPC server (previous experience was with REST only)
- **SQLite with Hibernate** - Custom dialect, sequence generation workarounds, H2 compatibility

The AI acted as a domain expert, providing not just code but also explanations and alternatives.

### 6. Pragmatic Simplification Decisions

AI was instructed to implement simplified versions of complex features to keep the project focused:

- **Payment Gateway** - Stub service returning fixed values instead of real integration
- **Idempotency Hashing** - Simple Base64 encoding instead of cryptographic hashing
- **Database** - SQLite (in-memory for demo) instead of production-grade PostgreSQL
- **Security** - No TLS/mTLS/auth (explicitly scoped out)

These decisions were human-driven, but AI generated appropriate implementations and documented the limitations clearly.

### 7. Documentation as Code

AI generated comprehensive documentation in parallel with code:

- **README.md** - Quick start, build/test/run instructions, database limitations
- **ARCHITECTURE.md** - System design, data model, idempotency strategy, error model
- **IMPLEMENTATION_NOTES.md** - Development approach, experience level, simplifications
- **AI_TRACE.md** - Structured trace of prompts, decisions, and verifications
- **Inline JavaDoc** - Only when explicitly requested for complex logic

### 8. Test-Driven Verification

AI generated tests in multiple categories:

- **Unit tests** - Service layer with mocks (@Mock, @InjectMocks)
- **Integration tests** - Spring Boot tests with real DB (H2)
- **Parameterized tests** - Validation rules with multiple input cases
- **Repository tests** - JPA repository methods with Flyway migrations
- **Client flow test** - End-to-end gRPC client simulation

Coverage was enforced via JaCoCo plugin with 80% threshold (pragmatic for demo project).

## Key Learnings and Observations

### What Worked Well

1. **Incremental building** - Starting simple and adding complexity prevented overwhelming errors
2. **Error-driven debugging** - Providing full stack traces led to accurate fixes
3. **Configuration standards** - copilot-instructions.md kept code style consistent
4. **AI as documentation writer** - Generated docs were comprehensive and accurate

### What Required Multiple Iterations

1. **JaCoCo exclusions** - Took several attempts to properly exclude generated code, DTOs, entities
2. **Flyway SQLite compatibility** - H2 vs SQLite syntax differences required refinement
3. **GitHub Actions secrets** - Understanding where/how secrets flow through CI took clarification
4. **Hibernate with SQLite** - Entity ID generation strategies needed custom configuration

### Human Oversight Critical For

1. **Architectural boundaries** - AI needs clear guidance on package structure
2. **Security scope** - AI will implement features unless told explicitly to skip
3. **Simplification decisions** - AI tends toward production-grade; human must define "good enough"
4. **Error interpretation** - Sometimes AI fixes symptoms; human must identify root cause

## Conclusion

This project demonstrates effective AI-assisted development where:
- Human provides architecture, constraints, and standards via copilot-instructions.md
- AI generates implementation, tests, tooling, and documentation
- Tight feedback loops ensure quality and correctness
- Result is a complete, working, well-tested, documented system built in 4 days

The key to success was clear prompting, incremental progress, and knowing when to simplify scope.

