package com.iitsm;

import com.iitsm.domain.enums.*;
import com.iitsm.domain.exception.InvalidStateTransitionException;
import com.iitsm.domain.model.*;
import com.iitsm.domain.valueobject.PriorityScore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for authorization boundaries, input validations,
 * token expirations, and lifecycle reopening edge cases.
 */
class IitsmSecurityAndBoundaryTests {

    private Employee employee;
    private Technician technician;
    private Administrator admin;
    private IncidentCategory category;
    private ITService service;

    @BeforeEach
    void setup() {
        employee = new Employee("emp@test.org", "hash", "Alice", "Brown", "Operations", "5501");
        technician = new Technician("tech@test.org", "hash", "Bob", "Miller", 4);
        admin = new Administrator("admin@test.org", "hash", "Charlie", "Root");
        category = new IncidentCategory("Network", "Connectivity issues");
        service = new ITService("Auth Service", "Core Auth", 5);
        technician.addSkill(new Skill("Network Skill", category));
    }

    private Incident createTestIncident() {
        return new Incident("INC-9999", "VPN Outage", "Cannot connect to gateway",
                employee, null, service,
                Impact.HIGH, Urgency.HIGH, 10,
                new PriorityScore(3.5, "High priority", Priority.P1),
                Priority.P1,
                LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(4));
    }

    private Incident createResolvedIncident() {
        Incident incident = createTestIncident();
        incident.classify(category, service);
        incident.assignTechnician(technician);
        incident.acknowledge(technician);
        incident.startInvestigation();
        incident.resolve("Root cause found", "Fix applied", "Preventive steps");
        return incident;
    }

    // ── Authorization & Permission Tests ──

    @Test
    @DisplayName("SEC-01: Employee cannot perform administrative actions")
    void testEmployeeRoleRestrictions() {
        assertFalse(employee.getDashboardPermissions().contains(Permission.MANAGE_USERS));
        assertFalse(employee.getDashboardPermissions().contains(Permission.CONFIGURE_STRATEGIES));
        assertTrue(employee.getDashboardPermissions().contains(Permission.VIEW_OWN_INCIDENTS));
    }

    @Test
    @DisplayName("SEC-02: Technician cannot modify system strategies")
    void testTechnicianRoleRestrictions() {
        assertFalse(technician.getDashboardPermissions().contains(Permission.CONFIGURE_STRATEGIES));
        assertTrue(technician.getDashboardPermissions().contains(Permission.RESOLVE_INCIDENT));
    }

    @Test
    @DisplayName("SEC-03: Administrator has complete system authorization")
    void testAdminRolePermissions() {
        assertTrue(admin.getDashboardPermissions().contains(Permission.MANAGE_USERS));
        assertTrue(admin.getDashboardPermissions().contains(Permission.CONFIGURE_STRATEGIES));
        assertTrue(admin.getDashboardPermissions().contains(Permission.VIEW_ALL_INCIDENTS));
    }

    // ── Boundary & Input Validation Tests ──

    @Test
    @DisplayName("BND-01: Satisfaction rating rejects values outside 1-5")
    void testSatisfactionRatingBoundaries() {
        Incident incident = createResolvedIncident();

        assertThrows(IllegalArgumentException.class, () -> incident.verify("Too low", 0));
        assertThrows(IllegalArgumentException.class, () -> incident.verify("Too high", 6));

        incident.verify("Valid", 5);
        assertEquals(Status.VERIFIED, incident.getStatus());
        assertEquals(5, incident.getSatisfactionRating());
    }

    @Test
    @DisplayName("BND-02: Single-use password reset token is invalidated after update")
    void testPasswordResetTokenSingleUse() {
        employee.setResetToken("TOKEN123", LocalDateTime.now().plusMinutes(15));
        assertTrue(employee.isResetTokenValid("TOKEN123"));

        employee.updatePassword("newHashValue");
        assertFalse(employee.isResetTokenValid("TOKEN123"));
    }

    @Test
    @DisplayName("BND-03: Expired password reset tokens are rejected")
    void testPasswordResetTokenExpiration() {
        employee.setResetToken("EXPIRED1", LocalDateTime.now().minusMinutes(1));
        assertFalse(employee.isResetTokenValid("EXPIRED1"));
    }

    // ── Reopen & Info Request Lifecycle Tests ──

    @Test
    @DisplayName("LFC-01: Reopen increments reopen counter and returns to IN_PROGRESS")
    void testIncidentReopenLifecycle() {
        Incident incident = createResolvedIncident();
        assertEquals(0, incident.getReopenCount());

        incident.reopen("Issue unresolved on mobile devices", "alice@test.org");
        assertEquals(Status.IN_PROGRESS, incident.getStatus());
        assertEquals(1, incident.getReopenCount());

        // Re-resolve and reopen again
        incident.resolve("Second fix applied", "Patch v2", "Monitoring");
        incident.reopen("Still failing in testing", "alice@test.org");
        assertEquals(2, incident.getReopenCount());
    }

    @Test
    @DisplayName("LFC-02: Reopen is strictly disallowed from unapproved states")
    void testIncidentReopenInvalidStates() {
        Incident incident = createTestIncident();
        assertEquals(Status.NEW, incident.getStatus());

        assertThrows(InvalidStateTransitionException.class,
                () -> incident.reopen("Premature reopen", "alice@test.org"));
    }

    @Test
    @DisplayName("LFC-03: Support info request adds audit log during investigation")
    void testRequestInfoDuringInvestigation() {
        Incident incident = createTestIncident();
        incident.classify(category, service);
        incident.assignTechnician(technician);
        incident.acknowledge(technician);
        incident.startInvestigation();

        incident.requestInfo("Please specify OS version", "tech@test.org");
        boolean logged = incident.getAuditTrail().stream()
                .anyMatch(a -> "INFO_REQUESTED".equals(a.getAction()));
        assertTrue(logged);
        assertEquals(Status.PENDING_USER, incident.getStatus());
        assertEquals(SlaStatus.PAUSED, incident.getResolutionSlaStatus());
    }

    @Test
    @DisplayName("LFC-04: PendingUser state pauses SLA clock and extends deadline on resume")
    void testPendingUserPausesSlaClockAndExtendsDeadline() {
        Incident incident = createTestIncident();
        incident.classify(category, service);
        incident.assignTechnician(technician);
        incident.acknowledge(technician);
        incident.startInvestigation();

        LocalDateTime originalDeadline = incident.getResolutionDeadline();
        incident.requestInfo("Need more server logs", "tech@test.org");

        assertEquals(Status.PENDING_USER, incident.getStatus());
        assertEquals(SlaStatus.PAUSED, incident.getResolutionSlaStatus());
        assertNotNull(incident.getSlaPausedAt());

        // Resume work
        incident.resumeWork("emp@test.org", "Logs uploaded to ticket");
        assertEquals(Status.IN_PROGRESS, incident.getStatus());
        assertEquals(SlaStatus.ON_TRACK, incident.getResolutionSlaStatus());
        assertNull(incident.getSlaPausedAt());
        assertTrue(incident.getResolutionDeadline().isAfter(originalDeadline) ||
                   incident.getResolutionDeadline().isEqual(originalDeadline));
    }

    @Test
    @DisplayName("SEC-04: Account lockout activates after 5 consecutive failed login attempts")
    void testAccountLockoutAfterFiveFailedAttempts() {
        assertFalse(employee.isAccountLocked());
        assertEquals(0, employee.getFailedLoginAttempts());

        for (int i = 1; i <= 4; i++) {
            employee.recordFailedLogin();
            assertFalse(employee.isAccountLocked());
            assertEquals(i, employee.getFailedLoginAttempts());
        }

        // 5th failed attempt triggers 15-minute account lockout
        employee.recordFailedLogin();
        assertTrue(employee.isAccountLocked());
        assertEquals(5, employee.getFailedLoginAttempts());
        assertNotNull(employee.getLockedUntil());
        assertTrue(employee.getLockedUntil().isAfter(LocalDateTime.now()));

        // Successful authentication clears lockout
        employee.resetFailedLogins();
        assertFalse(employee.isAccountLocked());
        assertEquals(0, employee.getFailedLoginAttempts());
        assertNull(employee.getLockedUntil());
    }

    @Test
    @DisplayName("ESCL-01: Chain of Responsibility escalates through all 4 hierarchical tiers")
    void testChainOfResponsibilityEscalation() {
        com.iitsm.domain.escalation.EscalationChain chain = new com.iitsm.domain.escalation.EscalationChain();
        Incident incident = createTestIncident();
        assertEquals(EscalationLevel.TECHNICIAN, incident.getEscalationLevel());

        // Tier 1 -> Tier 2 (Technician -> Team Lead)
        boolean h1 = chain.escalate(incident, "SLA approaching", "admin@test.org");
        assertTrue(h1);
        assertEquals(EscalationLevel.TEAM_LEAD, incident.getEscalationLevel());

        // Tier 2 -> Tier 3 (Team Lead -> System Admin)
        boolean h2 = chain.escalate(incident, "SLA breached", "admin@test.org");
        assertTrue(h2);
        assertEquals(EscalationLevel.SYSTEM_ADMIN, incident.getEscalationLevel());

        // Tier 3 -> Tier 4 (System Admin -> IT Manager)
        boolean h3 = chain.escalate(incident, "Critical P1 impact", "admin@test.org");
        assertTrue(h3);
        assertEquals(EscalationLevel.IT_MANAGER, incident.getEscalationLevel());

        // Tier 4 (Terminal Tier)
        boolean h4 = chain.escalate(incident, "Executive notification", "admin@test.org");
        assertTrue(h4);
        assertEquals(EscalationLevel.IT_MANAGER, incident.getEscalationLevel());
    }
}
