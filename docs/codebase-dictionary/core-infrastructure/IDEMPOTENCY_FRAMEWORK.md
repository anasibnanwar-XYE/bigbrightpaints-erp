# Core Infrastructure: Idempotency Framework

This document covers all idempotency classes in `com.bigbrightpaints.erp.core.idempotency`.

## Overview

Idempotency classes provide support for safe duplicate operations with key normalization, signature building, and atomic reservations. They handle idempotency at both the HTTP header and request body levels.

---

## IdempotencyReservationService

| Field | Value |
|-------|-------|
| **Name** | IdempotencyReservationService |
| **Type** | Service (Spring Bean) |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.idempotency |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/idempotency/IdempotencyReservationService.java |
| **Responsibility** | Manages idempotency key reservations with atomic guarantees and payload mismatch detection |
| **Use when** | Implementing idempotent operations |
| **Do not use when** | Simple key-only idempotency |
| **Public methods** | `String normalizeKey(String raw)`<br>`String requireKey(String raw, String label)`<br>`boolean isDataIntegrityViolation(Throwable error)`<br>`ApplicationException payloadMismatch(String idempotencyKey)`<br>`<T> void assertAndRepairSignature(T record, String idempotencyKey, String expectedSignature, Function<T, String> signatureExtractor, BiConsumer<T, String> signatureSetter, Consumer<T> signatureSaver)`<br>`<T> Reservation<T> reserve(Supplier<Optional<T>> existingLookup, Supplier<T> reservationCreator)` |
| **Callers** | Services implementing idempotent operations |
    **Dependencies** | IdempotencyUtils, ApplicationException, ErrorCode |
    **Side effects** | Database writes (via signatureSaver) |
    **Invariants protected** | Max key length 128; atomic reservation; signature mismatch detection |
    **Status** | Canonical |

### Reservation Record

| Field | Type | Description |
|-------|------|-------------|
| leader | boolean | True if this request created the reservation |
| record | T | The entity (existing or newly created) |

### Usage Pattern

```java
Reservation<MyEntity> reservation = idempotencyService.reserve(
    () -> repository.findByIdempotencyKey(key),
    () -> repository.save(newEntity)
);

if (reservation.leader()) {
    // This request created the entity
    processNewEntity(reservation.record());
} else {
    // Entity already exists (concurrent request)
    returnExistingEntity(reservation.record());
}
```

---

## IdempotencySignatureBuilder

| Field | Value |
|-------|-------|
| **Name** | IdempotencySignatureBuilder |
| **Type** | Utility (final class) |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.idempotency |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/idempotency/IdempotencySignatureBuilder.java |
| **Responsibility** | Builds normalized signatures for idempotency payload matching |
| **Use when** | Creating signatures for idempotent operations |
| **Do not use when** | Simple key-based idempotency |
| **Public methods** | `static IdempotencySignatureBuilder create()`<br>`IdempotencySignatureBuilder add(Object value)`<br>`IdempotencySignatureBuilder addToken(String value)`<br>`IdempotencySignatureBuilder addUpperToken(String value)`<br>`IdempotencySignatureBuilder addAmount(BigDecimal value)`<br>`IdempotencySignatureBuilder addDecimal(BigDecimal value)`<br>`String buildPayload()`<br>`String buildHash()`<br>`String buildHash(int length)` |
    **Callers** | Services implementing payload-based idempotency |
    **Dependencies** | IdempotencyUtils |
    **Side effects** | None |
    **Invariants protected** | Consistent signature format; normalized values |
    **Status** | Canonical |

### Usage Pattern

```java
String signature = IdempotencySignatureBuilder.create()
    .addUpperToken(dealerCode)
    .addAmount(amount)
    .addDecimal(taxAmount)
    .buildHash();  // SHA-256 hex

// Store signature on entity for later comparison
entity.setIdempotencySignature(signature);
```

---

## IdempotencyUtils

| Field | Value |
|-------|-------|
| **Name** | IdempotencyUtils |
| **Type** | Utility (final class) |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.idempotency |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/idempotency/IdempotencyUtils.java |
| **Responsibility** | Low-level idempotency utilities for normalization and hashing |
| **Use when** | Normalizing idempotency values |
    **Do not use when** | N/A |
| **Public methods** | `static String normalizeKey(String raw)`<br>`static String normalizeToken(String value)`<br>`static String normalizeUpperToken(String value)`<br>`static String normalizeAmount(BigDecimal value)`<br>`static String normalizeDecimal(BigDecimal value)`<br>`static String sha256Hex(String value)`<br>`static String sha256Hex(byte[] value)`<br>`static String sha256Hex(String value, int length)`<br>`static boolean isDataIntegrityViolation(Throwable error)` |
    **Callers** | IdempotencyReservationService, IdempotencySignatureBuilder |
    **Dependencies** | Apache Commons Codec (DigestUtils) |
    **Side effects** | None |
    **Invariants protected** | SHA-256 hashing; null-safe operations |
    **Status** | Canonical |

### Normalization Methods

| Method | Description |
|--------|-------------|
| normalizeKey | Trims whitespace, returns null if empty |
| normalizeToken | Trims whitespace, returns empty string if null |
| normalizeUpperToken | Trims and uppercases |
| normalizeAmount | Strips trailing zeros, returns plain string |
| normalizeDecimal | Strips trailing zeros, returns "0" if null |
| sha256Hex | SHA-256 hash in hex format |
| isDataIntegrityViolation | Detects DataIntegrityViolationException in cause chain |
