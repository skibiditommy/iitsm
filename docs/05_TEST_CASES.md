# 05 — Test Cases

## Test Suite: `IitsmDomainTests.java`

**Framework:** JUnit 5  
**Location:** `backend/src/test/java/com/iitsm/IitsmDomainTests.java`  
**Total Tests:** 27  
**Coverage:** Domain model, state transitions, strategy algorithms, workload invariants, escalation, audit trail, comments, value objects, RBAC polymorphism, blast radius.

---

## Test Case Table

| ID | Test Name | Description | Input / Setup | Expected Output | Status |
|----|-----------|-------------|---------------|-----------------|--------|
| T-01 | `fullIncidentLifecycle_HappyPath` | Verifies the complete lifecycle: NEW → CLASSIFIED → ASSIGNED → ACKNOWLEDGED → IN_PROGRESS → RESOLVED → VERIFIED → CLOSED | Create incident, classify, assign technician, acknowledge, start investigation, resolve, verify, close | Each transition succeeds; final status = CLOSED | ✅ Written |
| T-02 | `newState_onlyAllowsClassifyAndEscalate` | Ensures NEW state rejects invalid transitions | Attempt `acknowledge()` on NEW incident | Throws `InvalidStateTransitionException` | ✅ Written |
| T-03 | `classifiedState_onlyAllowsAssignAndEscalate` | Ensures CLASSIFIED state rejects invalid transitions | Attempt `resolve()` on CLASSIFIED incident | Throws `InvalidStateTransitionException` | ✅ Written |
| T-04 | `assignedState_onlyAllowsAcknowledgeEscalateReassign` | Ensures ASSIGNED state rejects invalid transitions | Attempt `close()` on ASSIGNED incident | Throws `InvalidStateTransitionException` | ✅ Written |
| T-05 | `inProgressState_rejectsClassify` | Ensures IN_PROGRESS state rejects classify | Attempt `classify()` on IN_PROGRESS incident | Throws `InvalidStateTransitionException` | ✅ Written |
| T-06 | `resolvedState_allowsVerify` | Verifies RESOLVED → VERIFIED transition | Call `verify()` on resolved incident | Status = VERIFIED | ✅ Written |
| T-07 | `closedState_rejectsAllTransitions` | CLOSED is a terminal state | Attempt `escalate()` on CLOSED incident | Throws `InvalidStateTransitionException` | ✅ Written |
| T-08 | `weightedScoringStrategy_calculatesCorrectPriority` | Tests weighted scoring algorithm | IMPACT_4, URGENCY_4, criticality=5, users=100 | Returns P1 priority with score > 3.0 | ✅ Written |
| T-09 | `weightedScoringStrategy_lowInputsGiveP4` | Tests low-severity scoring | IMPACT_1, URGENCY_1, criticality=1, users=1 | Returns P4 priority | ✅ Written |
| T-10 | `matrixRuleBasedStrategy_highImpactUrgency` | Tests ITIL 4×4 matrix | IMPACT_4, URGENCY_4, criticality=5, users=50 | Returns P1 priority | ✅ Written |
| T-11 | `matrixRuleBasedStrategy_lowImpactUrgency` | Tests ITIL 4×4 matrix low end | IMPACT_1, URGENCY_1, criticality=1, users=1 | Returns P4 priority | ✅ Written |
| T-12 | `skillBasedAssignment_matchesCorrectTechnician` | Tests skill-matching logic | Incident with "Network" category; 2 technicians — one has Network skill, one doesn't | Assigns technician with matching skill | ✅ Written |
| T-13 | `skillBasedAssignment_returnsEmptyWhenNoMatch` | Tests no-match scenario | Incident with "Security" category; no technician has Security skill | Returns `Optional.empty()` | ✅ Written |
| T-14 | `loadBasedAssignment_picksLeastLoaded` | Tests load-balancing logic | 3 technicians at 20%, 50%, 80% utilisation | Assigns the 20% utilisation technician | ✅ Written |
| T-15 | `roundRobinAssignment_cyclesThrough` | Tests round-robin rotation | 3 technicians; 4 consecutive assignments | Cycles: T1, T2, T3, T1 | ✅ Written |
| T-16 | `technicianWorkload_incrementDecrement` | Tests workload counter invariants | Technician with maxWorkload=5, currentWorkload=0 | After 3 increments: workload=3; after 1 decrement: workload=2 | ✅ Written |
| T-17 | `technicianWorkload_cannotExceedMax` | Enforces workload cap | Technician at max capacity (5/5); attempt increment | Throws `TechnicianWorkloadExceededException` | ✅ Written |
| T-18 | `technicianWorkload_cannotGoBelowZero` | Enforces workload floor | Technician at 0 workload; attempt decrement | Workload stays at 0 (no exception) | ✅ Written |
| T-19 | `slaTarget_immutability` | Value object is immutable | Create SlaTarget with response=15min, resolution=60min | Fields match; object is `@Embeddable` | ✅ Written |
| T-20 | `priorityScore_immutability` | Value object is immutable | Create PriorityScore with score=3.5, explanation, priority=P1 | Fields match; object is `@Embeddable` | ✅ Written |
| T-21 | `escalation_fromNewToEscalated` | Tests escalation from NEW | Call `escalate()` on NEW incident | Status = ESCALATED; escalation level = TIER_2 | ✅ Written |
| T-22 | `escalation_fromInProgressToEscalated` | Tests escalation from IN_PROGRESS | Call `escalate()` on IN_PROGRESS incident | Status = ESCALATED | ✅ Written |
| T-23 | `reassign_fromEscalatedToAssigned` | Tests reassignment after escalation | Escalate incident, then `reassign()` to new technician | Status = ASSIGNED; new technician assigned | ✅ Written |
| T-24 | `employeePermissions_limitedActions` | Tests RBAC polymorphism | Create Employee; call `getDashboardPermissions()` and `canPerformAction()` | Has VIEW_OWN_INCIDENTS; cannot MANAGE_USERS | ✅ Written |
| T-25 | `technicianPermissions_workActions` | Tests RBAC polymorphism | Create Technician; call `canPerformAction()` | Can RESOLVE_INCIDENT; cannot MANAGE_USERS | ✅ Written |
| T-26 | `adminPermissions_fullAccess` | Tests RBAC polymorphism | Create Administrator; call `getDashboardPermissions()` | Has all permissions including MANAGE_USERS, CONFIGURE_STRATEGIES | ✅ Written |
| T-27 | `auditTrail_recordsTransitions` | Tests audit log composition | Create incident, classify, assign; check audit trail | Audit trail has 3+ entries with correct action descriptions | ✅ Written |
| T-28 | `serviceBlastRadius_bfsTraversal` | Tests BFS impact propagation | Auth Service downstream traversal | Accurately identifies blast radius set | ✅ Written |
| T-29 | `serviceDependencyExplorer_cascadingPaths` | Verifies cascading graph paths | Auth Service -> Email Service -> CRM Application | Traces exact dependency paths | ✅ Written |
| T-30 | `rootCauseSuggestion_ranksUpstreamCulprits` | Evaluates shared cause ranking | 4 active incidents on downstream dependents | Auth Service ranks #1 with highest impact score | ✅ Written |
| T-31 | `employeeRoleRestrictions` | Tests authorization bounds | Employee attempts admin config | Forbidden / lacks MANAGE_USERS | ✅ Written |
| T-32 | `technicianRoleRestrictions` | Tests technician authorization bounds | Technician attempts strategy updates | Forbidden / lacks CONFIGURE_STRATEGIES | ✅ Written |
| T-33 | `adminRolePermissions` | Tests complete admin RBAC | Administrator checks full privileges | All administrative permissions granted | ✅ Written |
| T-34 | `satisfactionRatingBoundaries` | Enforces 1-5 feedback rating bounds | Input values 0 and 6 | Throws IllegalArgumentException | ✅ Written |
| T-35 | `passwordResetTokenSingleUse` | Enforces single-use token lifecycle | Update password, attempt token reuse | Returns false; token invalidated | ✅ Written |
| T-36 | `passwordResetTokenExpiration` | Enforces 15-minute token expiry | Test expired timestamp | Returns false; expired token rejected | ✅ Written |
| T-37 | `incidentReopenLifecycle` | Verifies incident reopening | Employee rejects resolution on RESOLVED ticket | Status returns to IN_PROGRESS; reopenCount increments | ✅ Written |
| T-38 | `incidentReopenInvalidStates` | Reopen guard verification | Attempt reopen on NEW ticket | Throws InvalidStateTransitionException | ✅ Written |
| T-39 | `requestInfoDuringInvestigation` | Verifies technician clarification requests | Technician requests info on IN_PROGRESS ticket | Logs INFO_REQUESTED audit trail entry | ✅ Written |
| T-40 | `pendingUserPausesSlaClockAndExtendsDeadline` | Verifies state pattern SLA clock pause & extension | Technician requests info (`PENDING_USER`), user resumes (`IN_PROGRESS`) | Status transitions; SLA paused; deadline extended by paused duration | ✅ Written |
| T-41 | `accountLockoutAfterFiveFailedAttempts` | Verifies brute-force defense lockout | 5 consecutive invalid login attempts on user account | User locked for 15 minutes; resets upon valid login | ✅ Written |
| T-42 | `chainOfResponsibilityEscalation` | Verifies GoF Chain of Responsibility across 4 tiers | Stalled incident passed to `EscalationChain` | Handled through Technician → Team Lead → Admin → IT Manager handlers | ✅ Written |

---

## Test Categories

| Category | Test IDs | Count |
|----------|----------|-------|
| **State Pattern (Lifecycle & SLA Pause)** | T-01 through T-07, T-40 | 8 |
| **Strategy Pattern (Priority)** | T-08 through T-11 | 4 |
| **Strategy Pattern (Assignment)** | T-12 through T-15 | 4 |
| **Encapsulation (Workload Invariants)** | T-16 through T-18 | 3 |
| **Value Objects (Immutability)** | T-19, T-20 | 2 |
| **Escalation & Chain of Responsibility** | T-21 through T-23, T-42 | 4 |
| **Polymorphism (RBAC & Security)** | T-24 through T-26, T-31 through T-33 | 6 |
| **Composition & Audit Trail** | T-27, T-39 | 2 |
| **Graph Algorithms & Root Cause** | T-28 through T-30 | 3 |
| **Boundary & Validation Rules** | T-34 through T-36, T-38 | 4 |
| **Incident Reopening Lifecycle** | T-37 | 1 |
| **Account Lockout & Brute-Force Defense** | T-41 | 1 |
| **Total Test Suite** | **All Categories** | **42** |

---

## Running the Tests

```bash
# From the backend directory
cd backend

# Run all tests
./mvnw test

# Run only domain tests
./mvnw test -Dtest=IitsmDomainTests

# Run a specific test
./mvnw test -Dtest=IitsmDomainTests#fullIncidentLifecycle_HappyPath
```

> **Note:** Java 21 (JDK) must be installed and on the PATH. The Maven wrapper (`mvnw`) handles Maven itself.
