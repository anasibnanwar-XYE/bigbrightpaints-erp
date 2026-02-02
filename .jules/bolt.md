## 2026-02-02 - Dockerless Testing Constraint
**Learning:** The sandbox environment does not support Docker/Testcontainers, causing `AbstractIntegrationTest` subclasses to fail.
**Action:** Prefer Unit Tests with Mockito for verifying logic and performance optimizations. Use `ReflectionTestUtils` to set IDs on entities that lack setters.
