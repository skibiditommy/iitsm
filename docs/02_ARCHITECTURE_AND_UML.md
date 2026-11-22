# 02 — Architecture & UML Diagrams

## 1. Layered Architecture

I-ITSM follows a strict **4-layer architecture** with unidirectional dependency flow:

```mermaid
flowchart TB
    subgraph Presentation["Presentation Layer"]
        direction LR
        React["React 18 / TypeScript / Vite"]
        REST["REST Controllers"]
        WS["WebSocket STOMP"]
        EH["GlobalExceptionHandler"]
    end

    subgraph Application["Application / Service Layer"]
        direction LR
        AuthSvc["AuthService"]
        IncSvc["IncidentService"]
        UserSvc["UserService"]
        SlaSvc["SlaService"]
        AnalyticsSvc["AnalyticsService"]
        NotifSvc["NotificationService"]
        SearchSvc["SearchService"]
        DemoSvc["DemoTimeService"]
    end

    subgraph Domain["Domain Layer — Rich OOP Core"]
        direction LR
        Entities["Entities & Value Objects"]
        StatePat["State Pattern"]
        StratPat["Strategy Pattern"]
        ObsPat["Observer Pattern"]
        FactPat["Factory Pattern"]
        Exceptions["Exception Hierarchy"]
    end

    subgraph Infrastructure["Infrastructure Layer"]
        direction LR
        Repos["JPA Repositories"]
        Adapters["Adapter Pattern"]
        Schedulers["Schedulers"]
        Seed["DataSeeder"]
    end

    subgraph Data["Data Layer"]
        H2["H2 File-backed DB"]
        PG["PostgreSQL (Docker)"]
    end

    React -->|"HTTP / WebSocket"| Presentation
    Presentation --> Application
    Application --> Domain
    Application --> Infrastructure
    Infrastructure --> Data
```

**Key Design Decisions:**
- The **Domain Layer** has zero dependencies on Spring, JPA annotations excepted. Business logic lives in entities, not services.
- The **Service Layer** orchestrates but does not own business rules — it delegates to domain objects.
- The **Infrastructure Layer** implements interfaces defined in the Domain via the Adapter pattern.

---

## 2. Package Structure

```
com.iitsm/
├── IitsmApplication.java                # Spring Boot entry point
├── config/                              # Security, WebSocket, Strategy registry
│   ├── JwtTokenProvider.java
│   ├── JwtAuthenticationFilter.java
│   ├── SecurityConfig.java
│   ├── WebSocketConfig.java
│   └── StrategyConfigService.java
├── controller/                          # REST controllers
│   ├── AuthController.java
│   ├── IncidentController.java
│   ├── UserController.java
│   ├── TechnicianController.java
│   ├── ServiceController.java
│   ├── CategoryController.java
│   ├── SlaPolicyController.java
│   ├── StrategyController.java
│   ├── AnalyticsController.java
│   ├── DemoController.java
│   ├── NotificationController.java
│   ├── AuditController.java
│   └── GlobalExceptionHandler.java
├── domain/
│   ├── enums/        (11 enums)
│   ├── valueobject/  (PriorityScore, SlaTarget)
│   ├── exception/    (IitsmException + 14 subclasses)
│   ├── model/        (12 entities)
│   ├── state/        (IncidentState + 8 states + factory)
│   ├── strategy/     (2 interfaces + 6 implementations)
│   ├── observer/     (10 events + publisher + 2 observers)
│   └── factory/      (IncidentFactory, UserFactory, NotificationFactory, NotificationPayload)
├── dto/
│   ├── request/      (16 request DTOs)
│   └── response/     (23 response DTOs)
├── infrastructure/
│   └── adapter/      (AI port + 2 adapters, Notification channel + 3 adapters)
├── repository/       (12 JPA repositories)
├── scheduler/        (SlaMonitorScheduler, EscalationScheduler)
├── seed/             (DataSeeder)
└── service/          (8 services)
```

---

## 3. Entity Class Diagram

```mermaid
classDiagram
    class User {
        <<abstract>>
        #Long id
        #String email
        #String passwordHash
        #String firstName
        #String lastName
        #Role role
        #boolean active
        +getFullName() String
        +getDashboardPermissions()* Set~Permission~
        +canPerformAction(Action)* boolean
    }

    class Employee {
        -String department
        -String phone
        +getDashboardPermissions() Set~Permission~
        +canPerformAction(Action) boolean
    }

    class Technician {
        -Set~Skill~ skills
        -int maxWorkload
        -int currentWorkload
        -int yearsOfExperience
        -boolean available
        +hasSkillFor(IncidentCategory) boolean
        +incrementWorkload()
        +decrementWorkload()
        +isOverloaded() boolean
    }

    class Administrator {
        -String adminLevel
        +getDashboardPermissions() Set~Permission~
        +canPerformAction(Action) boolean
    }

    User <|-- Employee : extends
    User <|-- Technician : extends
    User <|-- Administrator : extends

    class Incident {
        -Long id
        -String incidentId
        -String title
        -String description
        -IncidentState currentState
        -PriorityScore priorityScore
        -Priority calculatedPriority
        -EscalationLevel escalationLevel
        +classify(category, service)
        +assignTechnician(technician)
        +acknowledge(technician)
        +startInvestigation()
        +resolve(details)
        +verify(feedback, rating)
        +close()
        +escalate(level)
        +reassign(technician)
    }

    class ITService {
        -Long id
        -String name
        -int businessCriticality
        -Set~ITService~ upstreamDependencies
        -Set~ITService~ downstreamDependencies
        +getBlastRadius() Set~ITService~
    }

    class SlaPolicy {
        -Long id
        -Priority priority
        -Duration responseTime
        -Duration resolutionTime
    }

    class PriorityScore {
        <<value object>>
        -double score
        -String explanation
        -Priority priority
    }

    class SlaTarget {
        <<value object>>
        -Duration responseTime
        -Duration resolutionTime
    }

    Incident "*" --> "1" Employee : reporter
    Incident "*" --> "0..1" Technician : assignee
    Incident "*" --> "1" ITService : affectedService
    Incident "*" --> "1" IncidentCategory : category
    Incident *-- "0..*" AuditLogEntry : composition
    Incident *-- "0..*" Comment : composition
    Incident -- PriorityScore : embeds
    Technician o-- "0..*" Skill : aggregation
    ITService o-- "0..*" ITService : dependencies
    SlaPolicy --> Priority
```

---

## 4. State Machine Diagram — Incident Lifecycle (10 Domain States)

```mermaid
stateDiagram-v2
    [*] --> NEW
    NEW --> CLASSIFIED : classify()
    CLASSIFIED --> ASSIGNED : assignTechnician()
    ASSIGNED --> ACKNOWLEDGED : acknowledge()
    ACKNOWLEDGED --> IN_PROGRESS : startInvestigation()
    IN_PROGRESS --> PENDING_USER : requestInfo(question) [pauses SLA clock]
    PENDING_USER --> IN_PROGRESS : resumeWork(response) [extends deadline]
    IN_PROGRESS --> RESOLVED : resolve(resolutionDetails)
    IN_PROGRESS --> ASSIGNED : reassign(technician)
    RESOLVED --> VERIFIED : verify(rating, feedback)
    RESOLVED --> IN_PROGRESS : rejectResolution() / reopen()
    VERIFIED --> CLOSED : close()

    NEW --> ESCALATED : escalate()
    CLASSIFIED --> ESCALATED : escalate()
    ASSIGNED --> ESCALATED : escalate()
    ACKNOWLEDGED --> ESCALATED : escalate()
    IN_PROGRESS --> ESCALATED : escalate()
    PENDING_USER --> ESCALATED : escalate()
    ESCALATED --> ASSIGNED : reassign(technician)
```

---

## 5. Use Case Diagram

```mermaid
flowchart LR
    subgraph Actors
        EMP["👤 Employee"]
        TECH["🔧 Technician"]
        ADMIN["⚙️ Administrator"]
        SYS["🤖 System"]
    end

    subgraph UseCases["I-ITSM Use Cases"]
        UC1["Register / Login"]
        UC2["Report Incident"]
        UC3["View My Incidents"]
        UC4["Add Comment"]
        UC5["Verify Resolution"]
        UC6["View Assignment Queue"]
        UC7["Acknowledge Incident"]
        UC8["Investigate & Resolve"]
        UC9["View AI Suggestions"]
        UC10["Manage Users & Services"]
        UC11["Configure SLA Policies"]
        UC12["Switch Strategies"]
        UC13["View Analytics"]
        UC14["Time Acceleration"]
        UC15["Auto-Classify & Prioritise"]
        UC16["Monitor SLA"]
        UC17["Auto-Escalate"]
        UC18["Send Notifications"]
    end

    EMP --- UC1
    EMP --- UC2
    EMP --- UC3
    EMP --- UC4
    EMP --- UC5
    TECH --- UC1
    TECH --- UC6
    TECH --- UC7
    TECH --- UC8
    TECH --- UC9
    ADMIN --- UC1
    ADMIN --- UC10
    ADMIN --- UC11
    ADMIN --- UC12
    ADMIN --- UC13
    ADMIN --- UC14
    SYS --- UC15
    SYS --- UC16
    SYS --- UC17
    SYS --- UC18
```

---

## 6. Sequence Diagram — Incident Creation Flow

```mermaid
sequenceDiagram
    actor E as Employee
    participant C as IncidentController
    participant S as IncidentService
    participant F as IncidentFactory
    participant PS as PriorityStrategy
    participant AI as AiAssistantPort
    participant R as IncidentRepository
    participant P as EventPublisher
    participant O as Observers

    E->>C: POST /incidents
    C->>S: createIncident(request)
    S->>F: createIncident(request, reporter, strategy, slaPolicy)
    F->>PS: calculate(impact, urgency, criticality, users)
    PS-->>F: PriorityScore
    F-->>S: Incident (NEW state)
    S->>AI: predictCategory(description)
    AI-->>S: CategoryPrediction
    S->>S: incident.classify(category, service)
    S->>R: save(incident)
    S->>P: publish(IncidentCreatedEvent)
    P->>O: onEvent(event)
    O->>O: AuditTrailObserver: persist audit entry
    O->>O: NotificationObserver: push WebSocket + outbox
    S-->>C: IncidentResponse
    C-->>E: 201 Created
```

---

## 7. OOP Concepts — Defense Map

This table maps every core OOP concept to its **concrete implementation** in I-ITSM:

| OOP Concept | Where Implemented | Explanation |
|-------------|-------------------|-------------|
| **Encapsulation** | `Incident.java` domain methods | No public setters. All state mutations go through `classify()`, `assignTechnician()`, `resolve()`, etc., which enforce preconditions and invariants. `Technician.incrementWorkload()` enforces the max-workload cap internally. |
| **Abstraction** | `PriorityCalculationStrategy`, `TechnicianAssignmentStrategy`, `AiAssistantPort`, `NotificationChannelAdapter`, `IncidentState` | Service layer depends on interfaces, never on concrete classes. Callers don't know if priority is computed via weighted scoring or matrix rules. |
| **Inheritance** | `User` → `Employee`, `Technician`, `Administrator` | Single-table JPA inheritance. Each subclass overrides `getDashboardPermissions()` and `canPerformAction()` to define role-specific behaviour. `IitsmException` → 14 specific exception subclasses with distinct HTTP status codes. |
| **Polymorphism** | Strategy pattern, State pattern, Observer pattern | `IncidentService` calls `strategy.calculate()` — the concrete algorithm is selected at runtime. `incident.classify()` delegates to `currentState.classify()` — each state class defines which transitions are valid. `publisher.publish(event)` iterates over all `IncidentEventObserver` implementations. |
| **Composition** | `Incident` ↔ `AuditLogEntry`, `Comment`, `PriorityScore`, `SlaTarget` | Incident *owns* its audit trail and comments (cascade ALL, orphan removal). `PriorityScore` and `SlaTarget` are `@Embeddable` value objects — no identity outside their parent. |
| **Aggregation** | `Technician` ↔ `Skill` | Technician has a set of skills, but skills can exist independently. Skills are shared across technicians. |
| **Association** | `Incident` → `Employee` (reporter), `Incident` → `Technician` (assignee), `Incident` → `ITService` | Simple references — the associated entity has its own lifecycle. |

---

## 8. Design Patterns — Defense Map

| Pattern | Interface / Base | Concrete Implementations | Purpose |
|---------|-----------------|-------------------------|---------|
| **State** (10 Domain States) | `IncidentState` | `NewState`, `ClassifiedState`, `AssignedState`, `AcknowledgedState`, `InProgressState`, `PendingUserState`, `ResolvedState`, `VerifiedState`, `ClosedState`, `EscalatedState` | Encapsulate lifecycle transition rules per state; eliminate giant if-else chains. Pauses SLA clock during `PendingUserState`. |
| **Strategy** | `PriorityCalculationStrategy`, `TechnicianAssignmentStrategy` | `WeightedScoringPriorityStrategy`, `MatrixRuleBasedPriorityStrategy`, `SkillBasedAssignmentStrategy`, `LoadBasedAssignmentStrategy`, `RoundRobinAssignmentStrategy`, `SmartCompositeRecommendationStrategy` | Interchangeable algorithms switchable at runtime by admin. |
| **Chain of Responsibility** | `EscalationHandler`, `BaseEscalationHandler` | `TechnicianEscalationHandler`, `TeamLeadEscalationHandler`, `AdminEscalationHandler`, `ItManagerEscalationHandler`, orchestrated by `EscalationChain` | Hierarchical 4-tier incident escalation without procedural if-else loops; each handler manages its threshold or delegates to successor. |
| **Observer** | `IncidentEventObserver`, `IncidentEvent` | `AuditTrailObserver`, `NotificationObserver`; 10 sealed event records | Decouple event producers from consumers; audit trail and notifications are added without modifying `IncidentService`. |
| **Factory** | — | `IncidentFactory`, `UserFactory`, `NotificationFactory`, `IncidentStateFactory` | Centralise complex creation logic (ID generation, priority computation, SLA deadline calculation, password hashing, state hydration). |
| **Adapter** | `AiAssistantPort`, `NotificationChannelAdapter` | `LocalNlpAdapter`, `GeminiApiAdapter`; `InAppWebSocketAdapter`, `EmailNotificationAdapter`, `SmsNotificationAdapter` | Isolate external service integration behind stable interfaces; swap implementations via Spring's `@ConditionalOnProperty`. |
| **Repository** | Spring Data JPA | 12 repository interfaces with custom JPQL queries | Abstract data access; domain model is persistence-agnostic with immutable audit log enforcement. |
