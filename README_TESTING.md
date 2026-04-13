# Testing Guide for Travelo Backend

This document provides guidance on testing the Travelo backend services.

## Test Types

### 1. Unit Tests

**Location**: `services/*/src/test/java/`

**Purpose**: Test individual components in isolation.

**Example**:
```bash
mvn test -pl services/post-service
```

**Framework**: JUnit 5, Mockito

### 2. Integration Tests

**Location**: `services/*/src/test/java/*/integration/`

**Purpose**: Test services with real dependencies (database, etc.) using Testcontainers.

**Requirements**: Docker must be running.

**Example**:
```bash
mvn test -pl services/post-service -Dtest=*IntegrationTest
```

**Framework**: JUnit 5, Testcontainers

### 3. Contract Tests

**Location**: `contract-tests/pact/`

**Purpose**: Verify API contracts between services.

**Setup**:
```bash
npm install @pact-foundation/pact
```

**Run**:
```bash
npm test -- contract-tests/pact/
```

**Framework**: Pact

### 4. Performance Tests

**Location**: `performance-tests/k6/`

**Purpose**: Load and stress testing.

**Setup**:
```bash
# Install k6
# Windows: choco install k6
# Mac: brew install k6
# Linux: see k6.io/docs/getting-started/installation/
```

**Run**:
```bash
k6 run performance-tests/k6/post-service-load-test.js
```

**Framework**: k6

## Running All Tests

### All Unit Tests
```bash
mvn test
```

### All Tests (including integration)
```bash
mvn verify
```

### With Coverage
```bash
mvn clean test jacoco:report
```

## Test Configuration

### Test Profiles

Each service has a `application-test.yml` for test configuration:
- Isolated test database
- Mock external services
- Reduced logging

### Testcontainers

Integration tests use Testcontainers for:
- PostgreSQL containers
- Redis containers
- Kafka containers

## Best Practices

1. **Isolation**: Each test should be independent
2. **Mocking**: Mock external dependencies in unit tests
3. **Real Dependencies**: Use Testcontainers for integration tests
4. **Coverage**: Aim for >80% code coverage
5. **Performance**: Keep test execution fast (<5 minutes total)

## CI/CD Integration

Tests run automatically in CI/CD pipeline:
- Unit tests on every commit
- Integration tests on pull requests
- Performance tests on releases

## Troubleshooting

### Tests Failing Locally

1. Ensure Docker is running (for integration tests)
2. Check test database connectivity
3. Verify test profile is active

### Slow Tests

1. Use `@MockBean` instead of real beans where possible
2. Use `@TestPropertySource` to override slow configurations
3. Check for unnecessary `@SpringBootTest` annotations

## Test Coverage

Generate coverage reports:
```bash
mvn clean test jacoco:report
```

View reports:
- HTML: `target/site/jacoco/index.html`
- XML: `target/site/jacoco/jacoco.xml`

## Continuous Improvement

- Review test failures regularly
- Update tests when APIs change
- Add tests for bug fixes
- Monitor test execution time

