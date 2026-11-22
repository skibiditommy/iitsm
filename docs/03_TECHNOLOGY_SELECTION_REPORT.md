# 03 — Technology Selection Report

This document evaluates 9 key technology and design decisions in the I-ITSM project, answering the standard capstone evaluation questions.

---

## Q1. Why Spring Boot 3.2 with Java 21?

| Criterion | Evaluation |
|-----------|-----------|
| **Language fit** | Java is the canonical OOP language for demonstrating encapsulation, inheritance, polymorphism, and abstraction. Java 21 adds sealed interfaces (used for our event hierarchy), records (used for DTOs and value objects), and pattern matching. |
| **Framework productivity** | Spring Boot auto-configures JPA, Security, WebSocket, and REST with minimal boilerplate, letting us focus on domain logic rather than plumbing. |
| **Ecosystem** | Spring Data JPA eliminates DAO boilerplate; Spring Security provides production-grade JWT auth; `@Scheduled` powers our SLA monitor and escalation jobs. |
| **Alternatives considered** | *Jakarta EE (too verbose)*, *Quarkus (less mature Spring Data equivalent)*, *Node.js/Express (weaker OOP story; no native interface/abstract class support)*. |
| **Decision** | Spring Boot 3.2 on Java 21 provides the best balance of OOP expressiveness, convention-over-configuration productivity, and ecosystem maturity. |

---

## Q2. Why H2 (file-backed) as the default database?

| Criterion | Evaluation |
|-----------|-----------|
| **Zero-config startup** | H2 runs embedded — no separate database server to install. The grader runs `./mvnw spring-boot:run` and the app works immediately. |
| **Persistence** | File-backed mode (`jdbc:h2:file:./iitsm-data`) persists data across restarts, unlike in-memory mode. |
| **Schema generation** | Spring's `ddl-auto: update` creates all tables from JPA annotations on first run. |
| **Production readiness** | A `docker-compose.yml` and `application-docker.yml` profile are provided for PostgreSQL 16, demonstrating that the architecture is DB-agnostic. |
| **Media storage architectural evolution** | Current implementation stores base64 media payloads directly in the database for frictionless zero-setup local execution and offline grading. In a production enterprise deployment, this would evolve to dedicated cloud object storage (Amazon S3 / Azure Blob Storage) with only signed URLs and metadata persisted in the database, avoiding database bloat and memory pressure. |
| **Alternatives considered** | *PostgreSQL-only (requires Docker or native install)*, *SQLite (no JPA dialect out of the box)*. |
| **Decision** | H2 for development and grading; PostgreSQL available via Docker profile for production parity. |

---

## Q3. Why JWT for authentication?

| Criterion | Evaluation |
|-----------|-----------|
| **Stateless architecture** | JWT tokens are self-contained — no server-side session storage needed. This aligns with REST's statelessness constraint. |
| **Role embedding** | The JWT payload carries the user's role, enabling `@PreAuthorize` guards without a database lookup on every request. |
| **Frontend integration** | The React frontend stores the JWT in `localStorage` and attaches it via an Axios interceptor — simple and standard. |
| **Implementation** | JJWT 0.12.6 provides type-safe token creation and validation with HMAC-SHA256 signing. |
| **Alternatives considered** | *Session cookies (stateful; harder for SPA)*, *OAuth2/OIDC (over-engineered for a capstone)*, *Basic Auth (no token expiry)*. |
| **Decision** | JWT provides a clean, stateless, role-aware authentication mechanism ideal for a Spring Boot + React SPA. |

---

## Q4. Why React 18 with TypeScript and Vite?

| Criterion | Evaluation |
|-----------|-----------|
| **Component model** | React's component architecture maps naturally to our three-role dashboard (Employee, Technician, Admin) with shared components (Navbar, Chatbot, IncidentDetail). |
| **Type safety** | TypeScript catches API contract mismatches at compile time (interface definitions for all API responses). |
| **Build speed** | Vite provides sub-second HMR and fast production builds (~2s for the entire frontend). |
| **Styling** | Tailwind CSS 4 enables rapid, consistent UI development with utility classes — no separate CSS files to maintain. |
| **Charting** | Recharts provides React-native, responsive, declarative chart components (PieChart, BarChart) for the admin analytics dashboard. |
| **Real-time** | `@stomp/stompjs` + `sockjs-client` provide WebSocket connectivity for real-time notifications. |
| **Alternatives considered** | *Angular (heavier; more boilerplate)*, *Vue (smaller ecosystem for enterprise charting)*, *plain HTML/JS (no component reuse)*. |
| **Decision** | React 18 + TypeScript + Vite + Tailwind delivers the fastest development cycle with strong type safety and ecosystem support. |

---

## Q5. Why WebSocket (STOMP) for notifications?

| Criterion | Evaluation |
|-----------|-----------|
| **Real-time push** | SLA breach warnings and incident assignment notifications must reach users immediately — polling would add latency and waste bandwidth. |
| **STOMP protocol** | Provides topic-based pub/sub (`/topic/notifications/{email}`) with clean semantics. Spring's `SimpMessagingTemplate` makes server-side push trivial. |
| **SockJS fallback** | Automatic fallback to HTTP long-polling for browsers or proxies that block WebSocket. |
| **Alternatives considered** | *Server-Sent Events (unidirectional; no native Spring support for per-user topics)*, *Polling (latency; wasted requests)*, *Firebase/Pusher (external dependency)*. |
| **Decision** | STOMP over WebSocket with SockJS fallback provides native Spring integration, per-user topic routing, and bidirectional capability. |

---

## Q6. Why the Strategy Pattern for priority and assignment?

| Criterion | Evaluation |
|-----------|-----------|
| **Open/Closed Principle** | Adding a new priority algorithm (e.g., ML-based) requires only a new class implementing `PriorityCalculationStrategy` — no existing code changes. |
| **Runtime switching** | `StrategyConfigService` maintains a registry of named strategies and exposes a REST endpoint for admins to switch the active one. The volatile reference swap takes effect on the next incident — no restart needed. |
| **Demonstrable polymorphism** | The admin dashboard shows all available strategies and the active one. During a demo, switching from "weighted-scoring" to "matrix-rule-based" visibly changes computed priorities. |
| **Concrete implementations** | 2 priority strategies (WeightedScoring, MatrixRuleBased) and 4 assignment strategies (SkillBased, LoadBased, RoundRobin, SmartComposite) — 6 total. |
| **Decision** | Strategy pattern is the textbook solution for interchangeable algorithms and directly demonstrates polymorphism + abstraction. |

---

## Q7. Why the Observer Pattern for domain events?

| Criterion | Evaluation |
|-----------|-----------|
| **Decoupling** | `IncidentService` publishes events without knowing who listens. `AuditTrailObserver` and `NotificationObserver` can be added, removed, or replaced independently. |
| **Sealed event hierarchy** | 10 event records implement the `IncidentEvent` sealed interface, enabling exhaustive pattern matching and type-safe handling. |
| **Cross-cutting concerns** | Audit logging and notification dispatch are cross-cutting — the Observer pattern keeps them out of the core service logic. |
| **Alternatives considered** | *Spring's ApplicationEvent (couples to Spring framework)*, *AOP (less explicit; harder to debug)*, *Direct method calls (tight coupling)*. |
| **Decision** | A custom Observer implementation with a sealed event hierarchy provides maximum OOP visibility for the capstone defense while maintaining clean decoupling. |

---

## Q8. Why the State Pattern for incident lifecycle?

| Criterion | Evaluation |
|-----------|-----------|
| **Elimination of conditionals** | Without the State pattern, every lifecycle method would need a `switch(status)` with 9 cases. The State pattern replaces this with polymorphic dispatch — each state class defines only valid transitions. |
| **Encapsulation** | Transition rules are encapsulated within each state. `InProgressState.classify()` throws `InvalidStateTransitionException` — the Incident doesn't need to know why. |
| **Extensibility** | Adding a new state (e.g., "PENDING_APPROVAL") requires one new class and updating `IncidentStateFactory` — no changes to existing state classes. |
| **9 states, 10+ transitions** | A complex enough lifecycle to genuinely benefit from the pattern (not a toy example). |
| **Decision** | The State pattern is the definitive OOP solution for lifecycle management and provides the strongest capstone defense talking point. |

---

## Q9. Why the Adapter Pattern for AI and notifications?

| Criterion | Evaluation |
|-----------|-----------|
| **Interface segregation** | `AiAssistantPort` defines three methods: `predictCategory()`, `suggestResolutions()`, `processNaturalLanguage()`. Callers never see HTTP clients, API keys, or NLP internals. |
| **Seamless fallback** | `LocalNlpAdapter` is `@Primary` and always works offline. `GeminiApiAdapter` activates only when `gemini.api.key` is set via `@ConditionalOnProperty`. Zero-failure guaranteed. |
| **Notification channels** | `NotificationChannelAdapter` provides a uniform `send(payload)` interface. Adding Slack or Teams requires only a new adapter — no service-layer changes. |
| **Testability** | In tests, we can substitute mock adapters without touching business logic. |
| **Decision** | The Adapter pattern isolates volatile external dependencies (AI APIs, messaging channels) behind stable domain interfaces, demonstrating abstraction + polymorphism. |
