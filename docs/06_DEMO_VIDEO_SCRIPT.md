# 06 — Demo Video Script

**Duration:** 15–20 minutes  
**Team:** 4 members (Member A, Member B, Member C, Member D)  
**Format:** Screen-share with live application running  

> Replace "Member A/B/C/D" with actual team member names before recording.

---

## Setup Before Recording

1. Start the backend:
   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```
2. Start the frontend:
   ```bash
   cd frontend
   npm run dev
   ```
3. Open browser at `http://localhost:5173`
4. Have 3 browser tabs/profiles ready for simultaneous logins.

---

## Part 1: Architecture Walkthrough (Member A — 4 minutes)

### Slide: Title
> "Welcome to the demo of Nexora I-ITSM — Intelligent IT Service Management and Incident Resolution System for Nexora Global Technologies. This is our capstone project for CS F213, Object-Oriented Programming."

### Slide: Architecture Diagram
> Show `docs/02_ARCHITECTURE_AND_UML.md` or a projected version of the layered architecture diagram.

**Talk through:**
1. **4-Layer Architecture:** Presentation → Application → Domain → Infrastructure
2. **Rich Domain Model:** "Our domain layer is NOT anemic. The `Incident` entity encapsulates all lifecycle transitions — `classify()`, `assignTechnician()`, `requestInfo()`, `resolve()`. There are no public setters that bypass invariants."
3. **9 Design Patterns in action:**
   - **State Pattern:** "10 concrete state classes govern the incident lifecycle. Each state defines which transitions are valid — no if-else chains. The `PENDING_USER` state formally pauses the SLA clock."
   - **Strategy Pattern:** "Priority (`MatrixRuleBasedPriorityStrategy`, `WeightedScoringPriorityStrategy`) and assignment algorithms are interchangeable at runtime."
   - **Chain of Responsibility Pattern:** "A 4-tier escalation hierarchy (`TechnicianEscalationHandler` → `TeamLeadEscalationHandler` → `AdminEscalationHandler` → `ItManagerEscalationHandler`) handles stalled incidents without procedural loops."
   - **Observer Pattern:** "Domain events decouple the core from audit logging and notifications. 10 sealed event types, 2 observers."
   - **Factory Pattern:** "IncidentFactory, UserFactory, and NotificationFactory centralise complex creation logic — ID generation, priority calculation, SLA deadline computation."
   - **Adapter Pattern:** "AI categorisation uses a LocalNlpAdapter by default, with a GeminiApiAdapter available via feature flag. Notification channels are similarly abstracted."
   - **Repository Pattern:** "12 Spring Data JPA repositories abstract data access with an immutable, append-only audit trail."
   - **Composite & Value Object Patterns:** "DAG blast radius and immutable embedded value objects (`PriorityScore`, `SlaTarget`)."
4. **OOP Pillars:** Encapsulation (domain methods & capacity invariants), Inheritance (`User` → `Employee`, `Technician`, `Administrator`), Polymorphism (Strategy/State/CoR dispatch), Abstraction (stable domain ports).

---

## Part 2: Employee Workflow Demo (Member B — 4 minutes)

### Login & Visual Theme Switching
1. Open `http://localhost:5173`
2. **Demonstrate Dark / Light Mode:** In the top header bar of the login page, toggle between Dark Mode and Light Mode with a single click. Show the ambient glowing slate theme and crisp enterprise light theme.
3. Click the **"Employee"** quick-login button (logs in as **Rahul Sharma**, `employee@iitsm.org`)
4. Show the Employee Dashboard with the incident list.
5. In the top navbar, click the theme toggle (Sun/Moon icon) to demonstrate live theme switching across dashboards without reloading.

### Report an Incident
1. Click **"Report Incident"**
2. **Demonstrate Voice Dictation:** Click **"Dictate Description"** (microphone icon). Speak: *"Payment processing failing for international cards with 500 error on gateway."* The transcript streams into the description.
3. **Demonstrate Live Duplicate Detection:** Type a title like "Payment processing down". Show the duplicate warning banner flagging existing open ticket `INC-1006`.
4. **Demonstrate Media Evidence Attachment:** Click **"Choose Media File"** and attach a screenshot image or error recording. The interactive media preview appears immediately.
5. Submit the incident.
6. **Narrate:** "Watch what happens automatically — the system invokes our `IncidentFactory`, which generates a sequence-backed ID (`INC-1016+`), calls the active `PriorityCalculationStrategy` to compute priority, and attaches SLA deadlines from `SlaPolicy`. An email confirmation is dispatched to the reporter's outbox."
7. Show the newly reported ticket appearing instantly at the top of the list.
8. **Demonstrate Multi-Criteria Search & Filtering:** Filter by Category: *Payment*, Priority: *P1*, SLA: *On Track*, and keyword search.

### View Incident Detail
1. Click on the new incident
2. **Point out:** Auto-generated incident ID, computed priority, SLA response/resolution deadlines, audit trail showing "Incident Created" and "Incident Classified" entries
3. **Narrate:** "Every state transition creates an immutable `AuditLogEntry` via our `AuditTrailObserver` — that's the Observer pattern at work."

### Add a Comment
1. Type a comment: "This is impacting our Q4 revenue targets. Please prioritise."
2. Click Post
3. Show it appears with attribution and timestamp

---

## Part 3: Technician Workflow Demo (Member C — 4 minutes)

### Login
1. Open a new tab/profile
2. Login as `tech.alex@iitsm.org` (**Alex Johnson**) / `Password@123` (or use quick-login)
3. Show the Technician Dashboard — priority-ranked assignment queue

### Acknowledge & Investigate
1. Click on an assigned incident (e.g. `INC-1016`).
2. Point out the attached media player and duplicate candidate warnings.
3. Click **"Acknowledge"** (ASSIGNED → ACKNOWLEDGED).
4. Click **"Start Investigation"** (ACKNOWLEDGED → IN_PROGRESS).
5. **Demonstrate Support Clarification Request:** Click **"Request Additional Info from Reporter"**, enter: *"Please confirm whether transactions fail on mobile app or web browser."* Show it logged to the audit trail and comment feed.

### View AI Suggested Resolutions
1. Scroll down to the **"🤖 AI Suggested Resolutions"** section.
2. **Narrate:** "Our `LocalNlpAdapter` computes Jaccard similarity between this incident and closed historical incidents, returning high-confidence resolution matches."

### Resolve the Incident & Employee Reopening Verification
1. Technician fills in Root Cause, Resolution, and Preventive Measures and clicks **"Submit Resolution"** (IN_PROGRESS → RESOLVED).
2. Switch back to Employee view:
   - Point out Employee verification options: **"Verify & Close"** (with 1-5 star rating) OR **"Reject Resolution & Reopen"**.
   - Demonstrate Reopening: Click **"Reject Resolution & Reopen"** with reason *"Still failing in staging"*.
   - Show status immediately returning to `IN_PROGRESS` and the **Reopened (1x)** badge appearing on the ticket!

---

## Part 4: Admin Dashboard & Advanced Features (Member D — 5 minutes)

### Login
1. Open a new tab/profile
2. Login as `admin@iitsm.org` (**Devon Vance**) / `Password@123`
3. Show the Admin Dashboard

### Analytics Overview
1. Show the **Overview** tab:
   - KPI cards: Total, Active, Resolved, SLA Breached
   - Priority pie chart
   - Category bar chart
2. **Narrate:** "These analytics are computed in real-time by `AnalyticsService` using custom JPQL queries across the incident repository."

### Technician Workloads
1. Switch to the **Workloads** tab
2. Show the utilisation gauges for each technician
3. **Narrate:** "Workload management is enforced by the `Technician` entity itself — `incrementWorkload()` checks against `maxWorkload` and throws `TechnicianWorkloadExceededException` if exceeded. That's encapsulation — the invariant lives in the domain object."

### Service Dependency Explorer & Automated Root Causes
1. Switch to the **Services & Root Causes** tab.
2. **Demonstrate Automated Root-Cause Suggestions:**
   - Show the *Possible Shared Causes* section ranking upstream services by how many downstream services have active incidents.
   - Point out that **Auth Service** ranks #1 with 4 downstream dependencies affected, providing a calculated impact ratio and high-confidence recommendation.
3. **Demonstrate Service Dependency Explorer:**
   - Select **Auth Service** in the dropdown.
   - Trace the cascading impact path: `Auth Service` ➔ `Email Service` ➔ `CRM Application`.
   - Click directly on the linked active CRM ticket (e.g. `INC-1006`) to open it.

### SLA Risk Detection & Admin Custom Deadlines
1. Switch to the **SLA Risk Detection** tab: Show active tickets proactively categorized as HIGH, MEDIUM, or LOW risk before breaches occur.
2. Open any active ticket as Admin:
   - In the **Admin: Set Technician Resolution Deadline** panel, select `+4 Hours` preset and enter an audit reason.
   - Click **Set & Dispatch Deadline**; show the target deadline updating live and logged to the permanent Audit Trail.

### Strategy Switching (Live Demo)
1. Switch to the **Strategies** tab.
2. Show current active strategies.
3. Click to switch Priority Strategy from "weighted-scoring" to "matrix-rule-based".
4. **Narrate:** "This calls `StrategyConfigService.setActivePriorityStrategy()` which swaps a volatile reference. The next incident created will use the matrix algorithm instead. No restart needed — that's the Strategy pattern with runtime switching."
5. Switch back to demonstrate it's instant.

### Simulation Mode & SLA Breach Auto-Escalation Demo
1. Point out the clearly labeled **"⚠️ Demo & Simulation Mode: Time Acceleration (Admin-Only)"** toolbar.
2. Click **"+30 min"**, then **"+60 min"**.
3. **Narrate:** "We've advanced simulated time by 90 minutes. Now watch what happens when we trigger the SLA check."
4. Show incidents transitioning to `AT_RISK` and `BREACHED`.
5. Point out that the `EscalationScheduler` automatically advances breached tickets across organizational tiers: `Technician` ➔ `Team Lead` ➔ `System Admin` ➔ `IT Manager`.

---

## Part 5: OOP Defense Summary (Member A — 2 minutes)

### Quick Recap Table

| Concept | Where |
|---------|-------|
| Encapsulation | `Incident` domain methods, `Technician.incrementWorkload()` |
| Inheritance | `User` → `Employee`, `Technician`, `Administrator`; `IitsmException` → 14 subclasses |
| Polymorphism | Strategy dispatch, State dispatch, Observer iteration |
| Abstraction | `PriorityCalculationStrategy`, `AiAssistantPort`, `NotificationChannelAdapter`, `IncidentState` |
| Composition | `Incident` *owns* `AuditLogEntry` and `Comment`; `PriorityScore` and `SlaTarget` are embedded |
| Aggregation | `Technician` *has* `Skill` (independent lifecycle) |

> "Every design decision in I-ITSM was made to demonstrate OOP principles in a real-world, production-grade context — not as toy examples. Thank you."

---

## Closing Notes

- **Total Java files:** ~172
- **Total frontend components:** 10 React files
- **Design patterns:** 6 (State, Strategy, Observer, Factory, Adapter, Repository)
- **JUnit tests:** 39 (100% passing across Domain, Impact, Security, and Boundary test suites)
- **Demo credentials:** All use password `Password@123`
  - Employee: `employee@iitsm.org`
  - Technician: `tech.alex@iitsm.org`
  - Admin: `admin@iitsm.org`
