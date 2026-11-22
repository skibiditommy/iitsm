# 04 — Database Design

## 1. ER Diagram

```mermaid
erDiagram
    USERS {
        BIGINT id PK
        VARCHAR user_type "EMPLOYEE | TECHNICIAN | ADMIN"
        VARCHAR email UK "NOT NULL"
        VARCHAR password_hash "NOT NULL"
        VARCHAR first_name
        VARCHAR last_name
        VARCHAR role "EMPLOYEE | TECHNICIAN | ADMIN"
        BOOLEAN active "DEFAULT true"
        INT failed_login_attempts "Default 0"
        TIMESTAMP locked_until "Null if active"
        TIMESTAMP created_at
        VARCHAR department "Employee only"
        VARCHAR phone "Employee only"
        INT max_workload "Technician only"
        INT current_workload "Technician only"
        INT years_of_experience "Technician only"
        BOOLEAN available "Technician only"
        VARCHAR admin_level "Admin only"
    }

    SKILLS {
        BIGINT id PK
        VARCHAR name UK "NOT NULL"
        BIGINT related_category_id FK
    }

    TECHNICIAN_SKILLS {
        BIGINT technician_id FK
        BIGINT skill_id FK
    }

    INCIDENT_CATEGORIES {
        BIGINT id PK
        VARCHAR name UK "NOT NULL"
        VARCHAR description
    }

    IT_SERVICES {
        BIGINT id PK
        VARCHAR name UK "NOT NULL"
        VARCHAR description
        INT business_criticality "1-5"
    }

    SERVICE_DEPENDENCIES {
        BIGINT upstream_id FK
        BIGINT downstream_id FK
    }

    SLA_POLICIES {
        BIGINT id PK
        VARCHAR priority "P1 | P2 | P3 | P4"
        BIGINT service_id FK "nullable - override"
        BIGINT response_time_minutes
        BIGINT resolution_time_minutes
    }

    INCIDENTS {
        BIGINT id PK
        VARCHAR incident_id UK "INC-XXXX"
        VARCHAR title "NOT NULL"
        TEXT description
        VARCHAR status "10 domain states"
        VARCHAR impact "IMPACT_1..4"
        VARCHAR urgency "URGENCY_1..4"
        INT affected_users_count
        DOUBLE priority_score
        VARCHAR priority_explanation
        VARCHAR calculated_priority "P1..P4"
        VARCHAR escalation_level
        BIGINT reporter_id FK
        BIGINT assigned_technician_id FK
        BIGINT category_id FK
        BIGINT affected_service_id FK
        BIGINT response_time_nanos "SlaTarget embedded"
        BIGINT resolution_time_nanos "SlaTarget embedded"
        VARCHAR response_sla_status
        VARCHAR resolution_sla_status
        TIMESTAMP response_deadline
        TIMESTAMP resolution_deadline
        TIMESTAMP responded_at
        TIMESTAMP resolved_at
        TIMESTAMP sla_paused_at "For PENDING_USER"
        INT total_sla_paused_minutes "Clock extension"
        INT reopen_count "Reopen tracking"
        VARCHAR root_cause
        TEXT resolution
        VARCHAR preventive_measures
        VARCHAR verification_feedback
        INT satisfaction_rating
        TIMESTAMP created_at
        TIMESTAMP closed_at
    }

    AUDIT_LOG_ENTRIES {
        BIGINT id PK
        BIGINT incident_id FK
        TIMESTAMP timestamp
        VARCHAR actor_email
        VARCHAR action
        VARCHAR previous_state
        VARCHAR new_state
        TEXT description
    }

    COMMENTS {
        BIGINT id PK
        BIGINT incident_id FK
        VARCHAR content
        BIGINT author_id FK
        BOOLEAN internal
        TIMESTAMP created_at
    }

    NOTIFICATION_OUTBOX {
        BIGINT id PK
        VARCHAR channel "IN_APP | EMAIL | SMS"
        VARCHAR recipient_email
        VARCHAR subject
        TEXT body
        BOOLEAN sent
        TIMESTAMP created_at
        TIMESTAMP sent_at
    }

    USERS ||--o{ INCIDENTS : "reports"
    USERS ||--o{ INCIDENTS : "assigned to"
    USERS ||--o{ COMMENTS : "authors"
    USERS ||--o{ TECHNICIAN_SKILLS : "has"
    SKILLS ||--o{ TECHNICIAN_SKILLS : "possessed by"
    INCIDENT_CATEGORIES ||--o{ SKILLS : "related to"
    INCIDENT_CATEGORIES ||--o{ INCIDENTS : "categorises"
    IT_SERVICES ||--o{ INCIDENTS : "affected"
    IT_SERVICES ||--o{ SERVICE_DEPENDENCIES : "upstream"
    IT_SERVICES ||--o{ SERVICE_DEPENDENCIES : "downstream"
    IT_SERVICES ||--o{ SLA_POLICIES : "overrides"
    INCIDENTS ||--o{ AUDIT_LOG_ENTRIES : "has"
    INCIDENTS ||--o{ COMMENTS : "has"
```

---

## 2. Table Descriptions

### 2.1 `USERS` (Single-Table Inheritance)

The `User` entity hierarchy (`Employee`, `Technician`, `Administrator`) is stored in a single table using JPA's `@Inheritance(strategy = SINGLE_TABLE)` with a `user_type` discriminator column. Nullable columns hold subclass-specific fields.

| Column | Type | Constraint | Description |
|--------|------|-----------|-------------|
| `id` | BIGINT | PK, AUTO | Surrogate key |
| `user_type` | VARCHAR | NOT NULL | Discriminator: `EMPLOYEE`, `TECHNICIAN`, `ADMIN` |
| `email` | VARCHAR | UNIQUE, NOT NULL | Login identifier |
| `password_hash` | VARCHAR | NOT NULL | BCrypt hash |
| `first_name`, `last_name` | VARCHAR | NOT NULL | Display name |
| `role` | VARCHAR | NOT NULL | Enum: `EMPLOYEE`, `TECHNICIAN`, `ADMIN` |
| `active` | BOOLEAN | DEFAULT TRUE | Soft delete flag |
| `department`, `phone` | VARCHAR | Nullable | Employee-specific |
| `max_workload`, `current_workload` | INT | Nullable | Technician-specific |
| `years_of_experience` | INT | Nullable | Technician-specific |
| `available` | BOOLEAN | Nullable | Technician-specific |
| `admin_level` | VARCHAR | Nullable | Administrator-specific |

### 2.2 `INCIDENTS`

Central table storing all incident data including embedded value objects (`PriorityScore`, `SlaTarget`) and resolution details.

**Key indexes:**
- `idx_incident_status` on `status` — for active-incident queries
- `idx_incident_priority` on `calculated_priority` — for queue ordering
- `idx_incident_reporter` on `reporter_id` — for "My Incidents" queries
- `idx_incident_technician` on `assigned_technician_id` — for assignment queue
- `idx_incident_sla` on `response_sla_status, resolution_sla_status` — for SLA breach queries

### 2.3 `AUDIT_LOG_ENTRIES`

Immutable append-only log of every incident state transition. Each entry records the actor, action, previous state, new state, and a human-readable description.

### 2.4 `NOTIFICATION_OUTBOX`

Transactional outbox pattern — notifications are written to this table atomically with the domain event, then dispatched asynchronously. The `sent` flag and `sent_at` timestamp track delivery.

### 2.5 `SERVICE_DEPENDENCIES`

Join table for the many-to-many self-referential relationship on `IT_SERVICES`. Used by `ITService.getBlastRadius()` BFS traversal.

---

## 3. Seed Data Summary

The `DataSeeder` component (`@Component` with `CommandLineRunner`) populates the database idempotently on startup:

| Entity | Count | Key Details |
|--------|-------|-------------|
| **Users** | 7 | 1 Employee (`employee@iitsm.org`), 2 extra employees, 3 Technicians (`tech.alex`, `tech.priya`, `tech.rohan` — different skills & workloads), 1 Admin (`admin@iitsm.org`) |
| **IT Services** | 7 | Payment Gateway, Core Database, Auth Service, Email Platform, CI/CD Pipeline, Monitoring Dashboard, API Gateway — with dependency links |
| **Categories** | 9 | Network, Hardware, Software, Security, Database, Cloud, Email, Access Management, Performance |
| **Skills** | 9 | One per category, linked via `related_category` FK |
| **SLA Policies** | 5 | P1 (15min/1hr), P2 (30min/4hr), P3 (1hr/8hr), P4 (4hr/24hr), Payment Gateway P1 override (10min/30min) |
| **Incidents** | 15+ | Spread across all 9 lifecycle states; 5 closed incidents with resolution details for AI suggestion matching |

All users have password: `Password@123` (BCrypt hashed).
