# 📚 Learning Journey - Lead Management System

This document captures the key learnings, concepts, and insights gained while building the Lead Management System.

---

## 🏗️ Hexagonal Architecture (Ports & Adapters)

### What I Learned

**Core Concept:** Separate business logic from infrastructure concerns by defining clear boundaries (ports) and implementations (adapters).

```
┌─────────────────────────────────────────────────────────┐
│                    DOMAIN LAYER                         │
│         (Pure business logic - no dependencies)         │
│   Lead, Email, Phone, VehicleInterest, ScoringRule     │
└─────────────────────────────────────────────────────────┘
                         ▲
┌─────────────────────────────────────────────────────────┐
│                  APPLICATION LAYER                       │
│              (Use cases & orchestration)                 │
│          LeadService, NotificationRouter                │
└─────────────────────────────────────────────────────────┘
                         ▲
┌─────────────────────────────────────────────────────────┐
│                   ADAPTER LAYER                          │
│            (Infrastructure implementations)              │
│   InMemoryRepository, TwilioAdapter, EmailAdapter       │
└─────────────────────────────────────────────────────────┘
```

### Key Takeaways

1. **Domain stays pure** - No framework dependencies (Spring, Hibernate) in domain layer
2. **Ports are interfaces** - Define what the domain needs (LeadPersistencePort, NotificationPort)
3. **Adapters are implementations** - Can be swapped without changing business logic
4. **Testability** - Domain can be tested without mocks; adapters are easily mockable

---

## 🎨 Design Patterns Learned

### 1. Strategy Pattern
**Problem:** Need multiple algorithms (scoring rules) that can be swapped at runtime.

**Solution:** Define interface (`ScoringRule`), implement variations, inject as a list.

```java
// Each rule is a strategy
public interface ScoringRule {
    double evaluate(Lead lead);
    double weight();
}

// Engine uses strategies without knowing implementations
public class LeadScoringEngine {
    private final List<ScoringRule> rules;  // Inject strategies
}
```

**Learning:** Open/Closed Principle - add new rules without modifying engine.

---

### 2. Factory Pattern
**Problem:** Object creation requires validation and complex setup.

**Solution:** Static factory methods encapsulate creation logic.

```java
// Factory method hides complexity
Lead lead = Lead.newLead(dealerId, tenantId, ...);
// Internally: validates, generates UUID, sets timestamps, initializes state
```

**Learning:** Factories give meaningful names and enforce invariants at creation time.

---

### 3. Builder Pattern
**Problem:** Objects with many parameters become hard to construct.

**Solution:** Fluent builder API for step-by-step construction.

```java
Notification notification = Notification.builder()
    .dealerId("dealer-1")
    .type(NotificationType.SMS)
    .body("Hello!")
    .to("+1234567890")
    .build();
```

**Learning:** Lombok's `@Builder` saves boilerplate; manual builders offer more control.

---

### 4. Adapter Pattern
**Problem:** External APIs (Twilio) have different interfaces than our domain.

**Solution:** Adapter wraps external API to match our port interface.

```java
public class TwilioSmsAdapter implements NotificationPort {
    // Adapts Twilio's Verification API to our NotificationPort
    public NotificationResult send(Notification n) {
        Verification.creator(serviceSid, n.getTo(), "sms").create();
        return NotificationResult.success("twilio", verificationSid);
    }
}
```

**Learning:** Adapters isolate external dependencies, making them replaceable.

---

### 5. Decorator Pattern
**Problem:** Add behavior (circuit breaker) without modifying existing adapters.

**Solution:** Wrap adapter with decorator that adds functionality.

```java
// Original adapter
NotificationPort smsAdapter = new TwilioSmsAdapter(config);

// Decorated with circuit breaker (same interface)
NotificationPort resilientAdapter = new CircuitBreakerNotificationAdapter(smsAdapter, breaker);
```

**Learning:** Decorators follow Single Responsibility - each class does one thing.

---

### 6. Repository Pattern
**Problem:** Domain shouldn't know about database implementation details.

**Solution:** Abstract persistence behind a collection-like interface.

```java
public interface LeadPersistencePort {
    Lead save(Lead lead);
    Optional<Lead> findByIdAndDealerId(String leadId, String dealerId);
}
```

**Learning:** Repository makes domain testable and storage-agnostic.

---

## 💎 Domain-Driven Design (DDD) Concepts

### Value Objects
**Immutable objects** defined by their attributes, not identity.

| Value Object | Why Value Object? |
|--------------|-------------------|
| `Email` | Two emails with same address are equal |
| `PhoneCoordinate` | Defined by countryCode + number |
| `VehicleInterest` | Defined by make/model/year |

**Learning:** Value objects enforce validation at construction and are thread-safe.

---

### Aggregates
**Lead** is the aggregate root - all access goes through it.

```java
lead.transitionTo(LeadState.CONTACTED, "sales-rep", "Initial call made");
// Lead manages its own state and audit trail
```

**Learning:** Aggregates protect invariants and encapsulate business rules.

---

## 🔄 Circuit Breaker Pattern

### State Machine
```
CLOSED ──[3 failures]──► OPEN ──[30s timeout]──► HALF_OPEN ──[success]──► CLOSED
           │                                          │
           └──────────────[failure]───────────────────┘
```

### What I Learned
1. **Fail fast** - Don't waste resources on failing services
2. **Self-healing** - Automatically recover when service is back
3. **Configurable** - Failure threshold and timeout are adjustable

---

## 🔒 Multi-Tenant Design

### Tenant Isolation Strategy
```java
// Composite key ensures isolation
String storageKey = dealerId + ":" + leadId;

// All queries scoped by dealer
List<Lead> findByDealerIdAndState(String dealerId, LeadState state);
```

**Learning:** Always include tenant ID in queries - never trust client input.

---

## 🧪 Testing Strategy

| Layer | Test Approach | Mocks Needed? |
|-------|---------------|---------------|
| Domain | Pure unit tests | No |
| Application | Mock ports | Yes (ports only) |
| Adapters | Integration tests | External APIs |

**Learning:** Hexagonal architecture makes testing straightforward.

---

## 📝 Key Takeaways

1. **Architecture matters** - Clean boundaries = maintainable code
2. **Patterns are tools** - Use when they solve real problems
3. **Tests give confidence** - 95%+ coverage catches regressions
4. **Immutability is power** - Value objects prevent bugs
5. **Fail gracefully** - Circuit breakers protect the system
6. **Document as you go** - Future you will thank present you

---

## 🚀 Skills Gained

- [x] Hexagonal Architecture implementation
- [x] Gang of Four design patterns (Strategy, Factory, Builder, Adapter, Decorator)
- [x] Domain-Driven Design concepts (Value Objects, Aggregates)
- [x] Resilience patterns (Circuit Breaker)
- [x] Multi-tenant data isolation
- [x] Comprehensive unit testing
- [x] Clean code practices
- [x] Technical documentation

---

*Built as part of Tekion Backend Training Program*

