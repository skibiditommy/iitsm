# I-ITSM — Intelligent IT Service Management & Incident Resolution System

> **CS F213 / MAC F212 — Object-Oriented Programming Capstone Project**
> **Java 21 • Spring Boot 3.3.4 • React 18 • TypeScript • Tailwind CSS • Docker • Swagger OpenAPI 3.0**
> **42 Automated Tests Passing (100% Pass Rate, 0 Failures)**

A production-grade, ITIL-aligned incident management and service continuity platform demonstrating **9 Gang of Four (GoF) design patterns**, all **4 OOP pillars**, and intelligent automation.

---

## 🚀 Quick Start

### Prerequisites
- **Java 21** (JDK) — [Adoptium Temurin 21](https://adoptium.net/)
- **Node.js 18+** — [NodeJS](https://nodejs.org/)
- **Maven 3.9+** or included `./mvnw` wrapper
- No external database installation needed (runs out-of-the-box with file-backed H2; Docker Compose provided for PostgreSQL).

### One-Command Run

**Terminal 1 — Backend:**
```bash
cd backend
mvn spring-boot:run
# Or using wrapper: ./mvnw spring-boot:run (mvnw.cmd on Windows)
```
- Backend REST API: **http://localhost:8080**
- Interactive Swagger / OpenAPI Documentation: **http://localhost:8080/swagger-ui.html**

**Terminal 2 — Frontend:**
```bash
cd frontend
npm install    # first time only
npm run dev
```
- Frontend UI: **http://localhost:5173**

---

### 🔑 Demo Personas & Logins

| Role | Email | Password | Key Capabilities |
|------|-------|----------|------------------|
| 👤 **Employee** | `employee@iitsm.org` | `Password@123` | Self-registration, voice dictation, media upload, AI chatbot, ticket tracking, satisfaction feedback |
| 🔧 **Technician** | `tech.alex@iitsm.org` | `Password@123` | Assignment queue, investigate, request info (pauses SLA clock), AI suggested resolutions, resolve |
| 🛡️ **Administrator** | `admin@iitsm.org` | `Password@123` | Root cause graph explorer, SLA policy editor, IT service & category creation, technician fleet controls, manual ticket reassignment, immutable audit log |

> The login page provides **1-click login profiles**, **Employee Self-Registration**, **2-step password recovery**, and **Dark/Light mode toggling**.

---

## 🌟 Key Architectural Features

- 🔄 **10-State Incident Lifecycle (GoF State Pattern):**
  - Formally manages: `NEW`, `CLASSIFIED`, `ASSIGNED`, `ACKNOWLEDGED`, `IN_PROGRESS`, `PENDING_USER`, `RESOLVED`, `VERIFIED`, `CLOSED`, and `ESCALATED`.
  - **SLA Clock Pausing in PENDING_USER:** When a technician requests clarification (`requestInfo`), the ticket enters `PENDING_USER` and the SLA clock is paused (`SlaStatus.PAUSED`). Upon employee response, the ticket returns to `IN_PROGRESS` and extends the SLA resolution deadline by the exact paused duration.
- ⚡ **Pluggable Strategy Pattern:**
  - **Priority Calculation:** Switchable between `MatrixRuleBasedPriorityStrategy` (ITIL 4×4 matrix) and `WeightedScoringPriorityStrategy` (multi-factor formula).
  - **Technician Assignment:** Switchable between `SkillBasedAssignmentStrategy`, `LoadBasedAssignmentStrategy`, `RoundRobinAssignmentStrategy`, and `SmartCompositeRecommendationStrategy`.
- 🪜 **Hierarchical 4-Tier Escalation (GoF Chain of Responsibility):**
  - Stalled tickets delegate through `EscalationChain`: `TechnicianEscalationHandler` → `TeamLeadEscalationHandler` → `AdminEscalationHandler` → `ItManagerEscalationHandler` without procedural `if-else` branching.
- 🎙️ **Voice-Based Incident Dictation:** Native browser Web Speech API dictation enabling hands-free incident reporting.
- 📎 **Diagnostic Media Attachments:** Upload screenshots, log files, or video evidence directly when reporting tickets.
- 🌐 **Topological Service Dependency Graph & BFS Blast Radius:**
  - Directed Acyclic Graph (DAG) modeling upstream/downstream microservice dependencies.
  - Breadth-First Search (BFS) computes cascading blast radius sizes.
- 🧠 **Automated Root-Cause Suggestions:**
  - Ranks upstream culprit services using an objective structural formula:
    $$\text{Score} = (\text{Criticality} \times 25) + (\text{Blast Radius} \times 15) + (\text{Active Incident Count} \times 20)$$
- 🔒 **Enterprise Defensive Security:**
  - Stateless JWT authentication with HMAC-SHA256 signing.
  - Adaptive BCrypt password hashing (cost 12).
  - **Account Lockout:** Locks account for 15 minutes after 5 consecutive failed logins; automatically resets on valid authentication.
  - **Immutable Audit Trail:** Append-only repository; records cannot be updated or deleted.
- 🌓 **System-Wide Dark / Light Mode:** Persistent theme toggle on login and dashboards with OS preference auto-detection.
- 🐳 **Docker Compose & GitHub Actions CI:**
  - Multi-stage Docker containerization (`docker-compose up --build`).
  - Automated CI workflow testing JDK 21 backend and Node 20 frontend builds.
- 📖 **OpenAPI 3.0 / Swagger UI:** Interactive API explorer accessible at `/swagger-ui.html`.

---

## 🧪 Comprehensive Automated Test Suite

Run the full automated test suite (42 tests):
```bash
cd backend
mvn test
```
All **42 tests pass with 0 failures**:
- `IitsmDomainTests` (27 tests): Complete lifecycle, strategies, encapsulation invariants, RBAC polymorphism, graph algorithms, value objects.
- `IitsmSecurityAndBoundaryTests` (12 tests): Account lockout, SLA pause in `PENDING_USER`, Chain of Responsibility 4-tier escalation, token lifecycle, satisfaction boundary checks, and incident reopening.
- `ServiceImpactTests` (3 tests): Service dependency graph cascading impact and root cause ranking.

---

## 📚 Capstone Documentation Deliverables

All required project documentation is located in the [`docs/`](file:///c:/Users/Aarya%20Y%20Deshpande/OneDrive/Desktop/OOPS%20project/iitsm/docs) directory:
- [`I-ITSM_Comprehensive_Capstone_Report.md`](file:///c:/Users/Aarya%20Y%20Deshpande/OneDrive/Desktop/OOPS%20project/iitsm/docs/I-ITSM_Comprehensive_Capstone_Report.md): **One unified, publication-ready master report** combining problem analysis, UML diagrams, technology selection (9 questions), ER database design, security architecture, 42 test cases, and demo scripts.
- [`01_PROBLEM_ANALYSIS_AND_REQUIREMENTS.md`](file:///c:/Users/Aarya%20Y%20Deshpande/OneDrive/Desktop/OOPS%20project/iitsm/docs/01_PROBLEM_ANALYSIS_AND_REQUIREMENTS.md): Functional and non-functional requirements, actor matrix, user workflows.
- [`02_ARCHITECTURE_AND_UML.md`](file:///c:/Users/Aarya%20Y%20Deshpande/OneDrive/Desktop/OOPS%20project/iitsm/docs/02_ARCHITECTURE_AND_UML.md): Layered architecture, class diagram, 10-state lifecycle diagram, sequence diagram, use case diagram, and design pattern defense map.
- [`03_TECHNOLOGY_SELECTION_REPORT.md`](file:///c:/Users/Aarya%20Y%20Deshpande/OneDrive/Desktop/OOPS%20project/iitsm/docs/03_TECHNOLOGY_SELECTION_REPORT.md): In-depth evaluation of 9 architectural decisions answering rubric criteria.
- [`04_DATABASE_DESIGN.md`](file:///c:/Users/Aarya%20Y%20Deshpande/OneDrive/Desktop/OOPS%20project/iitsm/docs/04_DATABASE_DESIGN.md): Entity-Relationship diagram, 3NF/BCNF normalization proof, and schema specification.
- [`05_TEST_CASES.md`](file:///c:/Users/Aarya%20Y%20Deshpande/OneDrive/Desktop/OOPS%20project/iitsm/docs/05_TEST_CASES.md): Full catalog of all 42 automated test cases with inputs and asserted outcomes.
- [`06_DEMO_VIDEO_SCRIPT.md`](file:///c:/Users/Aarya%20Y%20Deshpande/OneDrive/Desktop/OOPS%20project/iitsm/docs/06_DEMO_VIDEO_SCRIPT.md): Step-by-step evaluation walkthrough and recording guide for the 3 core workflows.
- [`07_SECURITY_CONSIDERATIONS.md`](file:///c:/Users/Aarya%20Y%20Deshpande/OneDrive/Desktop/OOPS%20project/iitsm/docs/07_SECURITY_CONSIDERATIONS.md): Comprehensive security analysis (threat model, BCrypt, account lockout, SLA clock pausing, RBAC, and cloud production evolution).
- 🧠 **Automated Root-Cause Suggestions**: Graph-based ranking of upstream services by affected downstream dependents (Auth Service ranks #1 with 4 affected services)
- ⏱️ **Admin Custom Deadlines**: Administrators can set custom SLA deadlines (+2h, +4h, +24h, or datetime picker) with audit trail tracking
- 🔑 **Forgot Password & Email Verification**: End-to-end password recovery with simulated email outbox verification codes
- 🔔 **Multi-Channel Notification Center**: In-app bell dropdown with unread badge count, email dispatch alerts, and SMS outbox
- 🧠 **AI Categorisation & Suggested Resolutions**: Keyword NLP and Jaccard similarity against historical resolved tickets
- 📊 **Real-Time Analytics & Workloads**: Incident distribution charts, technician capacity utilization, and health status
- ⏱️ **SLA Monitoring & Auto-Escalation**: 4-tier hierarchical escalation (Technician ➔ Team Lead ➔ System Admin ➔ IT Manager)
- 🔀 **Runtime Strategy Switching**: Dynamically switch Priority and Assignment strategies without server restart
- ⏩ **Time Acceleration Demo**: Instant simulation of SLA breaches (+5m, +15m, +30m, +60m)
- 🤖 **Conversational Chatbot**: Multi-turn support bot for self-service guidance and incident logging

---

## 🎬 Feature Demo Walkthrough

| Feature | How to demo it in the UI |
|---|---|
| **Voice-based incident reporting** | 1. Sign in as Employee (`employee@iitsm.org`).<br>2. Click **Report Incident**.<br>3. Click **Dictate Description** (microphone button). Speak into your microphone; the transcript appends live to the description. |
| **Media Attachments (Picture/Video)** | 1. In **Report Incident**, click **Choose Media File**.<br>2. Select any screenshot image (`.png`/`.jpg`) or screen video (`.mp4`/`.webm`).<br>3. Preview appears instantly. Click **Submit Incident**.<br>4. Open the created ticket in **Incident Detail** to view or download the attached media evidence. |
| **Service Dependency Explorer** | 1. Sign in as Admin (`admin@iitsm.org`).<br>2. Navigate to the **Services & Root Causes** tab.<br>3. Under **Service Dependency Explorer**, select **Auth Service**.<br>4. Trace the cascading path: `Auth Service` ➔ `Email Service` ➔ `CRM Application`.<br>5. Click on the linked CRM incident ticket badge (e.g. `INC-1006`) to open it directly. |
| **Automated Root-Cause Suggestions** | 1. In **Admin → Services & Root Causes**, view the **Possible Shared Causes** section.<br>2. Upstream services are automatically ranked by how many downstream services have active incidents.<br>3. **Auth Service** ranks #1 with 4 downstream services affected and 99% confidence. |
| **Admin Custom Deadline Management** | 1. Sign in as Admin (`admin@iitsm.org`).<br>2. Open any active ticket.<br>3. In the **Admin: Set Technician Resolution Deadline** card, click `+4 Hours` or pick a custom date/time, add an audit note, and click **Set & Dispatch Deadline**.<br>4. The deadline updates immediately and a new entry is logged to the **Audit Trail**. |
| **Forgot Password & Email Reset** | 1. Open the Login page.<br>2. Click **Forgot Password?**.<br>3. Enter `employee@iitsm.org` and click **Send Verification Code**.<br>4. The simulated email outbox displays your verification code.<br>5. Enter the code and your new password to reset and log in immediately. |
| **Notification Center & Dispatch** | 1. Click the 🔔 **Notification Bell** in the top navigation bar.<br>2. View the outbox of IN_APP, EMAIL, and SMS notifications triggered across the system.<br>3. Reporting a new incident immediately logs and dispatches an email confirmation to the reporter. |

---

## 🏗️ Architecture

```
┌──────────────────────────────────────────────────┐
│  Presentation Layer                              │
│  React 18 + TypeScript + Vite + Tailwind CSS     │
│  REST Controllers + WebSocket STOMP              │
├──────────────────────────────────────────────────┤
│  Application Layer                               │
│  AuthService, IncidentService, SlaService,       │
│  AnalyticsService, NotificationService, ...      │
├──────────────────────────────────────────────────┤
│  Domain Layer (Rich OOP Core)                    │
│  Entities, State Pattern, Strategy Pattern,      │
│  Observer Pattern, Factory Pattern, Value Objects │
├──────────────────────────────────────────────────┤
│  Infrastructure Layer                            │
│  JPA Repositories, Adapter Pattern (AI + Notif), │
│  Schedulers, DataSeeder                          │
├──────────────────────────────────────────────────┤
│  Data Layer: H2 (default) / PostgreSQL (Docker)  │
└──────────────────────────────────────────────────┘
```

---

## 🎯 OOP Concepts Demonstrated

| Concept | Implementation |
|---------|---------------|
| **Encapsulation** | `Incident` exposes `classify()`, `resolve()`, etc. — no public setters. `Technician.incrementWorkload()` enforces max-workload invariant internally. |
| **Inheritance** | `User` (abstract) → `Employee`, `Technician`, `Administrator` with single-table JPA inheritance. `IitsmException` → 14 specific exception subclasses. |
| **Polymorphism** | Strategy dispatch (`strategy.calculate()`), State dispatch (`currentState.classify()`), Observer iteration (`observers.forEach(o -> o.onEvent(event))`). |
| **Abstraction** | Interfaces: `PriorityCalculationStrategy`, `TechnicianAssignmentStrategy`, `AiAssistantPort`, `NotificationChannelAdapter`, `IncidentState`. |
| **Composition** | `Incident` *owns* `AuditLogEntry[]` and `Comment[]` (cascade ALL, orphan removal). `PriorityScore` and `SlaTarget` are `@Embeddable` value objects. |
| **Aggregation** | `Technician` *has* `Skill[]` (skills exist independently). |

---

## 🧩 Design Patterns

| Pattern | Interface | Implementations |
|---------|-----------|-----------------|
| **State** | `IncidentState` | 9 state classes (New, Classified, Assigned, Acknowledged, InProgress, Resolved, Verified, Closed, Escalated) |
| **Strategy** | `PriorityCalculationStrategy`, `TechnicianAssignmentStrategy` | WeightedScoring, MatrixRuleBased; SkillBased, LoadBased, RoundRobin, SmartComposite |
| **Observer** | `IncidentEventObserver` | AuditTrailObserver, NotificationObserver (10 sealed event types) |
| **Factory** | — | IncidentFactory, UserFactory, NotificationFactory |
| **Adapter** | `AiAssistantPort`, `NotificationChannelAdapter` | LocalNlpAdapter, GeminiApiAdapter; WebSocket, Email, SMS adapters |
| **Repository** | Spring Data JPA | 12 repository interfaces with custom JPQL |

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------|
| **Backend** | Spring Boot 3.2.5, Java 21, Spring Data JPA, Spring Security (JWT), Spring WebSocket |
| **Frontend** | React 18, TypeScript, Vite 6, Tailwind CSS 4, Recharts, Lucide Icons |
| **Database** | H2 (file-backed, default) / PostgreSQL 16 (Docker profile) |
| **Auth** | JWT (JJWT 0.12.6), BCrypt password hashing |
| **Real-time** | STOMP over WebSocket with SockJS fallback |
| **AI** | Local NLP (keyword extraction, Jaccard/TF-IDF similarity) + optional Gemini API |
| **Testing** | JUnit 5 (27 tests) |

---

## 📁 Project Structure

```
iitsm/
├── backend/                    # Spring Boot application (~166 Java files)
│   ├── pom.xml
│   ├── src/main/java/com/iitsm/
│   │   ├── config/            # JWT, Security, WebSocket, Strategy registry
│   │   ├── controller/        # 12 REST controllers + exception handler
│   │   ├── domain/            # Rich domain model (entities, state, strategy, observer, factory, exceptions, enums, value objects)
│   │   ├── dto/               # 39 request/response DTOs
│   │   ├── infrastructure/    # Adapter pattern (AI + notification channels)
│   │   ├── repository/        # 12 JPA repositories
│   │   ├── scheduler/         # SLA monitor, auto-escalation
│   │   ├── seed/              # Idempotent demo data seeder
│   │   └── service/           # 8 service classes
│   └── src/test/              # 27 JUnit 5 tests
├── frontend/                   # React + TypeScript + Vite
│   └── src/
│       ├── components/        # Navbar, Chatbot
│       └── pages/             # Login, Employee, Technician, Admin, IncidentDetail
├── docs/                       # Academic documentation
│   ├── 01_PROBLEM_ANALYSIS_AND_REQUIREMENTS.md
│   ├── 02_ARCHITECTURE_AND_UML.md
│   ├── 03_TECHNOLOGY_SELECTION_REPORT.md
│   ├── 04_DATABASE_DESIGN.md
│   ├── 05_TEST_CASES.md
│   └── 06_DEMO_VIDEO_SCRIPT.md
└── README.md                   # This file
```

---

## 🧪 Running Tests

```bash
cd backend
./mvnw test
```

---

## 🐳 PostgreSQL (Optional)

```bash
cd backend
docker-compose up -d
./mvnw spring-boot:run -Dspring-boot.run.profiles=docker
```

---

## 📄 Documentation

Detailed academic documentation is in the [`docs/`](docs/) directory:

1. [Problem Analysis & Requirements](docs/01_PROBLEM_ANALYSIS_AND_REQUIREMENTS.md)
2. [Architecture & UML Diagrams](docs/02_ARCHITECTURE_AND_UML.md)
3. [Technology Selection Report](docs/03_TECHNOLOGY_SELECTION_REPORT.md)
4. [Database Design](docs/04_DATABASE_DESIGN.md)
5. [Test Cases](docs/05_TEST_CASES.md)
6. [Demo Video Script](docs/06_DEMO_VIDEO_SCRIPT.md)

---

## 📝 License

Academic project — BITS Pilani, CS F213 / MAC F212.
