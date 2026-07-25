# Testing Guide

This document provides comprehensive information about testing in the ICU Patient Chart project.

## Overview

The project uses a multi-layered testing strategy:

| Layer | Technology | Count | Location |
|-------|-----------|-------|----------|
| Backend Unit Tests | JUnit 5 + Mockito | 319 | `backend/src/test/java/` |
| Backend Integration Tests | Testcontainers + PostgreSQL | 79 | `backend/src/test/java/` (integration package) |
| Frontend Unit Tests | Vitest + React Testing Library | 316 | `frontend/src/test/` |
| E2E Tests | Playwright | 40 specs | `tests/specs/` |

**Total: 754+ tests**

## Running Tests

### Backend Tests

```bash
cd backend

# Run all unit tests (319 tests)
mvn test

# Run integration tests (79 tests, requires Docker)
mvn test -Pintegration-test

# Run specific test class
mvn test -Dtest=PrescriptionListServiceTest

# Run with coverage report
mvn clean verify
# Coverage report: backend/target/site/jacoco/index.html
```

### Frontend Tests

```bash
cd frontend

# Run all unit tests (316 tests)
npm test

# Run with coverage
npm test -- --coverage
# Coverage report: frontend/coverage/index.html

# Run specific test file
npx vitest run src/test/components/PrescriptionTable.test.tsx

# Watch mode
npm test -- --watch

# Type checking
npx tsc --noEmit
```

### E2E Tests

```bash
cd tests

# Run all E2E tests (40 specs)
npx playwright test

# Run specific project
npx playwright test --project=doctor-chromium

# Run specific spec file
npx playwright test specs/doctor/prescription-workflow.spec.ts

# Interactive UI mode
npx playwright test --ui

# View HTML report
npx playwright show-report
```

## Test Structure

### Backend Test Organization

```
backend/src/test/java/com/superhumans/
├── service/                          # Service layer tests
│   ├── PrescriptionListServiceTest.java    (15 tests)
│   ├── PrescriptionItemServiceTest.java    (13 tests)
│   ├── PrescriptionExecutionServiceTest.java (7 tests)
│   ├── VitalSignServiceTest.java           (9 tests)
│   ├── MedicineCatalogServiceTest.java     (7 tests)
│   ├── LogNotificationServiceTest.java     (5 tests)
│   └── ... (other service tests)
├── controller/                       # Controller layer tests
│   ├── PrescriptionControllerTest.java     (14 tests)
│   ├── VitalSignControllerTest.java        (6 tests)
│   └── ... (other controller tests)
├── integration/                      # Integration tests
│   ├── PrescriptionRepositoryTest.java
│   └── ... (other repository tests)
└── TestSecurityHelper.java           # JWT mocking utilities
```

### Frontend Test Organization

```
frontend/src/test/
├── components/                       # Component tests
│   ├── prescription/
│   │   ├── PrescriptionTable.test.tsx
│   │   ├── PrescriptionItemForm.test.tsx
│   │   ├── PrescriptionItemTable.test.tsx
│   │   ├── DayPartPlanner.test.tsx
│   │   ├── PrescriptionExecutionPanel.test.tsx
│   │   ├── VitalSignForm.test.tsx
│   │   ├── AllergyWarning.test.tsx
│   │   └── ClosePrescriptionDialog.test.tsx
│   ├── IntensiveCareCard.test.tsx
│   └── ... (other component tests)
├── api/
│   └── endpoints.test.ts             # API client tests
└── setup.ts                          # Test configuration
```

### E2E Test Organization

```
tests/specs/
├── doctor/
│   ├── prescription-workflow.spec.ts       (4 tests)
│   ├── prescriptions.spec.ts               (2 tests)
│   ├── dashboard.spec.ts
│   └── ... (other doctor specs)
├── nurse/
│   ├── prescription-execution.spec.ts      (3 tests)
│   ├── dashboard.spec.ts
│   └── ... (other nurse specs)
├── hod/
│   └── clinical-day-reopen.spec.ts
├── admin/
│   └── admin.spec.ts
└── login.spec.ts
```

## Test Coverage Requirements

### Backend Coverage (JaCoCo)

- **Instruction coverage**: ≥60% (overall)
- **Branch coverage**: ≥50% (overall)
- **New services**: ≥80% line coverage
- **New controllers**: ≥75% line coverage

Coverage reports are generated in CI and uploaded as artifacts:
- `backend/target/site/jacoco/index.html`
- Download from GitHub Actions: Artifacts → `jacoco-report`

### Frontend Coverage (Vitest)

- **Line coverage**: ≥75% for new components
- **Branch coverage**: ≥65% for new components
- **Function coverage**: ≥80% for new components

Coverage reports are generated in CI:
- `frontend/coverage/index.html`
- Download from GitHub Actions: Artifacts → `vitest-coverage`

## Test Data Management

### Backend Test Data

#### Unit Tests
- Use `@ExtendWith(MockitoExtension.class)`
- Mock all dependencies with `@Mock` and `@InjectMocks`
- No database access
- Fast execution (<1s per test)

#### Integration Tests
- Use Testcontainers PostgreSQL
- Fresh database per test class
- Use `data-test.sql` for seed data
- Run with `mvn test -Pintegration-test`

#### Test Data Fixtures
```java
// Example: Creating test data
PrescriptionList list = PrescriptionList.builder()
    .id(UUID.randomUUID())
    .patientId(2002L)
    .status(PrescriptionListStatus.Saved)
    .build();
list.setCreatedBy(0L);
list.setUpdatedBy(0L);
```

### Frontend Test Data

#### Component Tests
- Mock API calls with `vi.mock()`
- Use test fixtures for mock data
- Isolate component behavior

```typescript
// Example: Mocking API
vi.mock('../../api/endpoints', () => ({
  prescriptionApi: {
    getByPatient: vi.fn().mockResolvedValue({ data: [] }),
    create: vi.fn().mockResolvedValue({ data: { id: 'test-id' } }),
  },
}));
```

#### E2E Tests
- Use Playwright fixtures with role-based authentication
- Tests are isolated and run in parallel
- Each test gets a fresh browser context

```typescript
// Example: Role-based test
test('doctor creates prescription', async ({ page }) => {
  await page.goto('/doctor/prescriptions?patientId=3001');
  // Test implementation
});
```

## Writing Good Tests

### Backend Best Practices

1. **Test names should describe behavior**
   ```java
   @Test
   void should_create_prescription_list_with_saved_status() {
       // Given
       Long patientId = 2002L;
       
       // When
       PrescriptionList result = service.create(patientId);
       
       // Then
       assertThat(result.getStatus()).isEqualTo(PrescriptionListStatus.Saved);
       assertThat(result.getPatientId()).isEqualTo(patientId);
   }
   ```

2. **Use Arrange-Act-Assert pattern**
   ```java
   @Test
   void should_throw_exception_when_prescription_not_found() {
       // Arrange
       UUID nonExistentId = UUID.randomUUID();
       when(repository.findById(nonExistentId)).thenReturn(Optional.empty());
       
       // Act & Assert
       assertThatThrownBy(() -> service.getById(nonExistentId))
           .isInstanceOf(NotFoundException.class);
   }
   ```

3. **Test edge cases**
   - Null values
   - Empty collections
   - Boundary conditions
   - Error scenarios
   - Concurrent modifications

### Frontend Best Practices

1. **Test user interactions, not implementation**
   ```typescript
   it('should call onSubmit when form is submitted', async () => {
     const onSubmit = vi.fn();
     render(<PrescriptionItemForm onSubmit={onSubmit} />);
     
     await userEvent.click(screen.getByRole('button', { name: /добавити/i }));
     
     expect(onSubmit).toHaveBeenCalled();
   });
   ```

2. **Use accessible queries**
   ```typescript
   // Good
   screen.getByRole('button', { name: /зберегти/i });
   screen.getByLabelText('Препарат');
   
   // Avoid
   screen.getByText('Зберегти');
   screen.getByTestId('submit-button');
   ```

3. **Mock API calls, not components**
   ```typescript
   // Good
   vi.mock('../../api/endpoints', () => ({
     prescriptionApi: { create: vi.fn() },
   }));
   
   // Avoid
   vi.mock('../../components/PrescriptionTable', () => ({
     default: () => <div>Mocked</div>,
   }));
   ```

## CI/CD Pipeline

### GitHub Actions Workflow

The CI pipeline runs on every push and PR:

```
┌─────────────────┐
│  Code Quality   │  Checkstyle + Oxlint + TypeScript
└────────┬────────┘
         │
    ┌────┴────┬──────────┬──────────┐
    ▼         ▼          ▼          ▼
┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐
│Backend │ │Backend │ │Frontend│ │  E2E   │
│  Unit  │ │ Integra│ │  Unit  │ │Playwrgt│
│ Tests  │ │ Tests  │ │ Tests  │ │ Tests  │
└────────┘ └────────┘ └────────┘ └────────┘
    │         │          │          │
    └────┬────┴────┬─────┴────┬─────┘
         ▼         ▼          ▼
    ┌─────────────────────────────┐
    │      Build Artifacts        │  (main branch only)
    └─────────────────────────────┘
```

### Required Status Checks

All jobs must pass before merging:
- ✅ Code Quality (format-check)
- ✅ Backend Tests (backend-test)
- ✅ Backend Integration Tests (backend-integration)
- ✅ Frontend Tests (frontend-test)
- ✅ E2E Tests (e2e-test)

### Build Artifacts

On `main` branch push, artifacts are built and stored for 30 days:
- `backend-jar`: `patient-chart-backend-*.jar`
- `frontend-dist`: `frontend/dist/` directory

## Troubleshooting

### Common Issues

#### Backend: "Connection refused" in integration tests
```bash
# Ensure Docker is running
docker ps

# Restart Docker if needed
sudo systemctl restart docker
```

#### Frontend: "Timeout calling onTaskUpdate"
```bash
# This is a known Vitest issue with slow tests
# Run with fewer workers
npx vitest run --pool=forks --poolOptions.forks.singleFork
```

#### E2E: Tests fail locally but pass in CI
```bash
# CI uses a fresh database. Reset your local DB:
psql -U postgres -d my_fullstack_db -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"

# Then restart backend
cd backend && mvn spring-boot:run
```

#### Playwright: "Browser closed unexpectedly"
```bash
# Reinstall browsers
cd tests
npx playwright install --with-deps chromium
```

### Test Execution Times

| Test Suite | Typical Duration | CI Timeout |
|------------|-----------------|------------|
| Backend Unit (319) | ~15s | 5min |
| Backend Integration (79) | ~45s | 10min |
| Frontend Unit (316) | ~2min | 10min |
| E2E (40 specs) | ~10min | 40min |

## Adding New Tests

### Backend Service Test Template

```java
package com.superhumans.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MyServiceTest {
    @Mock
    private MyRepository repository;
    
    @InjectMocks
    private MyService service;
    
    @Test
    void should_do_something() {
        // Arrange
        // ...
        
        // Act
        var result = service.doSomething();
        
        // Assert
        assertThat(result).isNotNull();
    }
}
```

### Frontend Component Test Template

```typescript
import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import MyComponent from './MyComponent';

describe('MyComponent', () => {
  it('should render correctly', () => {
    render(<MyComponent />);
    expect(screen.getByText(/expected text/i)).toBeInTheDocument();
  });
  
  it('should handle user interaction', async () => {
    const onClick = vi.fn();
    render(<MyComponent onClick={onClick} />);
    
    await userEvent.click(screen.getByRole('button'));
    
    expect(onClick).toHaveBeenCalled();
  });
});
```

### E2E Test Template

```typescript
import { test, expect } from '../fixtures/index';

test.describe('Feature Name', () => {
  test('should do something', async ({ page }) => {
    await page.goto('/doctor/prescriptions');
    
    // Perform action
    await page.getByRole('button', { name: /create/i }).click();
    
    // Verify result
    await expect(page.getByText(/success/i)).toBeVisible();
  });
});
```

## Resources

- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Vitest Documentation](https://vitest.dev/)
- [Playwright Documentation](https://playwright.dev/)
- [React Testing Library](https://testing-library.com/docs/react-testing-library/intro/)
- [Testcontainers](https://www.testcontainers.org/)

## Support

For testing-related questions or issues:
1. Check this guide first
2. Review existing tests for examples
3. Consult the [AGENTS.md](../AGENTS.md) file
4. Create a GitHub issue if needed
