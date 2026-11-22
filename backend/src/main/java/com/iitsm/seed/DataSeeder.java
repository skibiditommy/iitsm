package com.iitsm.seed;

import com.iitsm.domain.enums.*;
import com.iitsm.domain.model.*;
import com.iitsm.domain.valueobject.PriorityScore;
import com.iitsm.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Idempotent data seeder: populates the database with demo data on startup.
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final IncidentCategoryRepository categoryRepository;
    private final ITServiceRepository serviceRepository;
    private final SkillRepository skillRepository;
    private final SlaPolicyRepository slaPolicyRepository;
    private final IncidentRepository incidentRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already seeded. Skipping.");
            return;
        }
        log.info("=== Seeding database ===");

        String pwd = passwordEncoder.encode("Password@123");

        // ── Users ──
        Employee emp1 = new Employee("employee@iitsm.org", pwd, "Rahul", "Sharma", "Finance", "9876543210");
        Employee emp2 = new Employee("priya.patel@iitsm.org", pwd, "Priya", "Patel", "Marketing", "9876543211");
        Employee emp3 = new Employee("amit.kumar@iitsm.org", pwd, "Amit", "Kumar", "Engineering", "9876543212");
        Technician tech1 = new Technician("tech.alex@iitsm.org", pwd, "Alex", "Johnson", 7);
        Technician tech2 = new Technician("tech.priya@iitsm.org", pwd, "Priya", "Nair", 4);
        Technician tech3 = new Technician("tech.rohan@iitsm.org", pwd, "Rohan", "Desai", 2);
        Administrator admin = new Administrator("admin@iitsm.org", pwd, "Devon", "Vance");

        userRepository.saveAll(List.of(emp1, emp2, emp3, tech1, tech2, tech3, admin));
        log.info("Created 7 users");

        // ── Categories ──
        IncidentCategory catNetwork   = new IncidentCategory("Network", "Network connectivity and infrastructure issues");
        IncidentCategory catHardware  = new IncidentCategory("Hardware", "Physical hardware failures and replacements");
        IncidentCategory catSoftware  = new IncidentCategory("Software", "Application bugs, crashes, and installation issues");
        IncidentCategory catSecurity  = new IncidentCategory("Security", "Security incidents, unauthorized access, malware");
        IncidentCategory catDatabase  = new IncidentCategory("Database", "Database connectivity, performance, and corruption");
        IncidentCategory catEmail     = new IncidentCategory("Email", "Email service disruptions and configuration");
        IncidentCategory catCloud     = new IncidentCategory("Cloud", "Cloud infrastructure and deployment issues");
        IncidentCategory catAuth      = new IncidentCategory("Authentication", "Login, SSO, and authentication system issues");
        IncidentCategory catPerf      = new IncidentCategory("Performance", "System performance and slowness issues");
        categoryRepository.saveAll(List.of(catNetwork, catHardware, catSoftware, catSecurity,
                catDatabase, catEmail, catCloud, catAuth, catPerf));
        log.info("Created 9 categories");

        // ── Skills ──
        Skill skillNetwork = new Skill("Network Administration", catNetwork);
        Skill skillHardware = new Skill("Hardware Repair", catHardware);
        Skill skillSoftware = new Skill("Software Troubleshooting", catSoftware);
        Skill skillSecurity = new Skill("Security Analysis", catSecurity);
        Skill skillDatabase = new Skill("Database Administration", catDatabase);
        Skill skillEmail = new Skill("Email Systems", catEmail);
        Skill skillCloud = new Skill("Cloud Operations", catCloud);
        Skill skillAuth = new Skill("Auth Systems", catAuth);
        Skill skillPerf = new Skill("Performance Tuning", catPerf);
        skillRepository.saveAll(List.of(skillNetwork, skillHardware, skillSoftware, skillSecurity,
                skillDatabase, skillEmail, skillCloud, skillAuth, skillPerf));

        // Assign skills to technicians
        tech1.addSkill(skillNetwork); tech1.addSkill(skillSecurity); tech1.addSkill(skillDatabase);
        tech1.addSkill(skillCloud); tech1.setMaxWorkload(6);
        tech2.addSkill(skillSoftware); tech2.addSkill(skillEmail); tech2.addSkill(skillAuth);
        tech2.addSkill(skillPerf); tech2.setMaxWorkload(5);
        tech3.addSkill(skillHardware); tech3.addSkill(skillNetwork); tech3.addSkill(skillSoftware);
        tech3.setMaxWorkload(4);
        userRepository.saveAll(List.of(tech1, tech2, tech3));
        log.info("Created 9 skills and assigned to technicians");

        // ── IT Services ──
        ITService svcCoroDB   = new ITService("Core Database", "Central PostgreSQL cluster", 5);
        ITService svcAuth     = new ITService("Auth Service", "SSO and authentication gateway", 5);
        ITService svcPayment  = new ITService("Payment Gateway", "Payment processing system", 5);
        ITService svcEmail    = new ITService("Email Service", "Corporate email platform (Exchange)", 4);
        ITService svcCRM      = new ITService("CRM Application", "Customer relationship management", 3);
        ITService svcVPN      = new ITService("VPN Gateway", "Remote access VPN", 4);
        ITService svcMonitor  = new ITService("Monitoring Platform", "Infrastructure monitoring (Grafana + Prometheus)", 3);
        serviceRepository.saveAll(List.of(svcCoroDB, svcAuth, svcPayment, svcEmail, svcCRM, svcVPN, svcMonitor));

        // Set dependencies: Payment depends on CoreDB and Auth
        svcPayment.addUpstreamDependency(svcCoroDB);
        svcPayment.addUpstreamDependency(svcAuth);
        // CRM depends on CoreDB and Email
        svcCRM.addUpstreamDependency(svcCoroDB);
        svcCRM.addUpstreamDependency(svcEmail);
        // Email depends on Auth
        svcEmail.addUpstreamDependency(svcAuth);
        // VPN depends on Auth
        svcVPN.addUpstreamDependency(svcAuth);
        serviceRepository.saveAll(List.of(svcPayment, svcCRM, svcEmail, svcVPN));
        log.info("Created 7 IT services with dependencies");

        // ── SLA Policies ──
        SlaPolicy slaP1 = new SlaPolicy(Priority.P1, null, 5, 30);
        SlaPolicy slaP2 = new SlaPolicy(Priority.P2, null, 15, 120);
        SlaPolicy slaP3 = new SlaPolicy(Priority.P3, null, 60, 480);
        SlaPolicy slaP4 = new SlaPolicy(Priority.P4, null, 240, 1440);
        SlaPolicy slaPaymentP1 = new SlaPolicy(Priority.P1, svcPayment, 3, 15);
        slaPolicyRepository.saveAll(List.of(slaP1, slaP2, slaP3, slaP4, slaPaymentP1));
        log.info("Created 5 SLA policies");

        // ── Seed Incidents ──
        LocalDateTime now = LocalDateTime.now();

        // 5 Closed incidents with resolution details (for Suggested Resolutions)
        Incident inc1 = Incident.forSeed("INC-1001", "VPN Connection Dropping Frequently",
                "VPN disconnects every 10-15 minutes during remote work. Affects all departments.",
                emp1, catNetwork, svcVPN, Impact.HIGH, Urgency.HIGH, 50,
                new PriorityScore(3.0, "Weighted: High impact + High urgency", Priority.P2),
                Priority.P2, Status.CLOSED, tech1,
                now.minusHours(48), now.minusHours(44),
                SlaStatus.ON_TRACK, SlaStatus.ON_TRACK,
                now.minusHours(47), EscalationLevel.TECHNICIAN,
                "MTU mismatch between VPN client and server gateway",
                "Adjusted MTU size to 1400 on VPN gateway. Updated client configuration templates.",
                "Automated MTU detection, added monitoring alerts for VPN tunnel stability",
                now.minusHours(45), "Issue resolved, VPN is stable now", 5, now.minusHours(44));

        Incident inc2 = Incident.forSeed("INC-1002", "Email Service Outage - Exchange Server Down",
                "Exchange server unreachable. No emails being sent or received company-wide.",
                emp2, catEmail, svcEmail, Impact.CRITICAL, Urgency.CRITICAL, 500,
                new PriorityScore(4.0, "Critical impact, critical urgency", Priority.P1),
                Priority.P1, Status.CLOSED, tech2,
                now.minusHours(72), now.minusHours(70),
                SlaStatus.ON_TRACK, SlaStatus.ON_TRACK,
                now.minusHours(71), EscalationLevel.TEAM_LEAD,
                "Exchange Information Store service corrupted after Windows Update",
                "Rolled back Windows Update KB5034441. Rebuilt Exchange Information Store. Restored from backup.",
                "Excluded Exchange server from automatic Windows Updates. Created pre-update snapshot policy.",
                now.minusHours(68), "Email service fully restored and stable", 4, now.minusHours(67));

        Incident inc3 = Incident.forSeed("INC-1003", "Database Connection Pool Exhausted",
                "CRM application throwing 'connection pool exhausted' errors during peak hours.",
                emp3, catDatabase, svcCoroDB, Impact.HIGH, Urgency.MEDIUM, 100,
                new PriorityScore(2.8, "High impact, medium urgency", Priority.P2),
                Priority.P2, Status.CLOSED, tech1,
                now.minusHours(96), now.minusHours(90),
                SlaStatus.ON_TRACK, SlaStatus.ON_TRACK,
                now.minusHours(95), EscalationLevel.TECHNICIAN,
                "Connection leak in CRM application code - connections not being returned to pool",
                "Fixed connection leak in CRM data access layer. Increased pool size from 20 to 50. Added connection timeout of 30 seconds.",
                "Added HikariCP connection pool monitoring. Set up alerts for pool usage > 80%.",
                now.minusHours(88), "Database connections stable, no more pool exhaustion", 5, now.minusHours(87));

        Incident inc4 = Incident.forSeed("INC-1004", "Password Reset Not Working for SSO Users",
                "SSO users cannot reset passwords. The reset link redirects to a 404 page.",
                emp1, catAuth, svcAuth, Impact.MEDIUM, Urgency.HIGH, 200,
                new PriorityScore(2.6, "Medium impact, high urgency", Priority.P2),
                Priority.P2, Status.CLOSED, tech2,
                now.minusHours(24), now.minusHours(20),
                SlaStatus.ON_TRACK, SlaStatus.ON_TRACK,
                now.minusHours(23), EscalationLevel.TECHNICIAN,
                "NGINX reverse proxy config was updated and lost the /auth/reset path mapping",
                "Restored NGINX configuration for /auth/reset endpoint. Verified all SSO paths.",
                "Added NGINX config to version control. Created automated config validation test.",
                now.minusHours(19), "Password reset works perfectly now", 5, now.minusHours(18));

        Incident inc5 = Incident.forSeed("INC-1005", "Printer Not Responding on 3rd Floor",
                "HP LaserJet 4250 on 3rd floor shows offline. Print jobs queuing up.",
                emp2, catHardware, null, Impact.LOW, Urgency.LOW, 15,
                new PriorityScore(1.0, "Low impact, low urgency", Priority.P4),
                Priority.P4, Status.CLOSED, tech3,
                now.minusHours(120), now.minusHours(96),
                SlaStatus.ON_TRACK, SlaStatus.ON_TRACK,
                now.minusHours(118), EscalationLevel.TECHNICIAN,
                "Network cable disconnected during office cleaning",
                "Reconnected network cable. Verified printer connectivity. Cleared print queue.",
                "Secured network cables with cable management clips. Added printer to network monitoring.",
                now.minusHours(100), "Printer working fine", 4, now.minusHours(96));

        // Active incidents in various states
        Incident inc6 = Incident.forSeed("INC-1006", "CRM Application Extremely Slow",
                "CRM takes 30+ seconds to load pages. Users reporting timeout errors during data entry.",
                emp3, catPerf, svcCRM, Impact.HIGH, Urgency.HIGH, 80,
                new PriorityScore(3.2, "High impact, high urgency, critical service", Priority.P1),
                Priority.P1, Status.IN_PROGRESS, tech2,
                now.plusMinutes(5), now.plusMinutes(30),
                SlaStatus.ON_TRACK, SlaStatus.ON_TRACK,
                now.minusMinutes(3), EscalationLevel.TECHNICIAN,
                null, null, null, null, null, null, null);

        Incident inc7 = Incident.forSeed("INC-1007", "Suspicious Login Attempts Detected",
                "Multiple failed login attempts from unknown IP addresses on the Auth Service.",
                emp1, catSecurity, svcAuth, Impact.CRITICAL, Urgency.CRITICAL, 0,
                new PriorityScore(4.0, "Critical security event", Priority.P1),
                Priority.P1, Status.ASSIGNED, tech1,
                now.plusMinutes(5), now.plusMinutes(30),
                SlaStatus.ON_TRACK, SlaStatus.ON_TRACK,
                null, EscalationLevel.TECHNICIAN,
                null, null, null, null, null, null, null);

        Incident inc8 = Incident.forSeed("INC-1008", "Cloud Deployment Pipeline Failing",
                "CI/CD pipeline for microservices deployment failing with Docker registry timeout.",
                emp3, catCloud, null, Impact.MEDIUM, Urgency.MEDIUM, 10,
                new PriorityScore(2.0, "Medium impact and urgency", Priority.P3),
                Priority.P3, Status.NEW, null,
                now.plusHours(1), now.plusHours(8),
                SlaStatus.ON_TRACK, SlaStatus.ON_TRACK,
                null, EscalationLevel.TECHNICIAN,
                null, null, null, null, null, null, null);

        Incident inc9 = Incident.forSeed("INC-1009", "Email Attachments Not Downloading",
                "Users in Marketing department cannot download email attachments larger than 5MB.",
                emp2, catEmail, svcEmail, Impact.MEDIUM, Urgency.LOW, 25,
                new PriorityScore(1.8, "Medium impact, low urgency", Priority.P3),
                Priority.P3, Status.CLASSIFIED, null,
                now.plusHours(1), now.plusHours(8),
                SlaStatus.ON_TRACK, SlaStatus.ON_TRACK,
                null, EscalationLevel.TECHNICIAN,
                null, null, null, null, null, null, null);

        Incident inc10 = Incident.forSeed("INC-1010", "Payment Gateway Intermittent 503 Errors",
                "Payment processing returning 503 errors sporadically. 1 in 10 transactions failing.",
                emp1, catSoftware, svcPayment, Impact.CRITICAL, Urgency.HIGH, 300,
                new PriorityScore(3.6, "Critical payment service", Priority.P1),
                Priority.P1, Status.ACKNOWLEDGED, tech1,
                now.plusMinutes(3), now.plusMinutes(15),
                SlaStatus.ON_TRACK, SlaStatus.ON_TRACK,
                now.minusMinutes(1), EscalationLevel.TECHNICIAN,
                null, null, null, null, null, null, null);

        Incident inc11 = Incident.forSeed("INC-1011", "Monitoring Dashboard Not Updating",
                "Grafana dashboards showing stale data. Prometheus scrape targets showing DOWN.",
                emp3, catSoftware, svcMonitor, Impact.MEDIUM, Urgency.MEDIUM, 5,
                new PriorityScore(2.0, "Medium impact on monitoring", Priority.P3),
                Priority.P3, Status.RESOLVED, tech2,
                now.plusHours(1), now.plusHours(8),
                SlaStatus.ON_TRACK, SlaStatus.ON_TRACK,
                now.minusHours(1), EscalationLevel.TECHNICIAN,
                "Prometheus service crashed due to OOM after log volume spike",
                "Increased Prometheus memory limit to 8GB. Configured log retention to 7 days.",
                "Set up memory alerts for Prometheus. Implemented log rotation.",
                now.minusMinutes(30), null, null, null);

        Incident inc12 = Incident.forSeed("INC-1012", "VPN Cannot Connect from Mac Devices",
                "Mac users unable to establish VPN connection after macOS Sonoma update.",
                emp2, catNetwork, svcVPN, Impact.MEDIUM, Urgency.HIGH, 40,
                new PriorityScore(2.6, "Medium impact, high urgency", Priority.P2),
                Priority.P2, Status.IN_PROGRESS, tech3,
                now.plusMinutes(15), now.plusHours(2),
                SlaStatus.ON_TRACK, SlaStatus.ON_TRACK,
                now.minusMinutes(10), EscalationLevel.TECHNICIAN,
                null, null, null, null, null, null, null);

        Incident inc13 = Incident.forSeed("INC-1013", "Database Replication Lag Increasing",
                "Read replica showing 30+ seconds of replication lag. Report queries returning stale data.",
                emp3, catDatabase, svcCoroDB, Impact.HIGH, Urgency.MEDIUM, 50,
                new PriorityScore(2.8, "High impact on data consistency", Priority.P2),
                Priority.P2, Status.ESCALATED, tech1,
                now.plusMinutes(15), now.plusHours(2),
                SlaStatus.AT_RISK, SlaStatus.ON_TRACK,
                now.minusMinutes(5), EscalationLevel.TEAM_LEAD,
                null, null, null, null, null, null, null);

        Incident inc14 = Incident.forSeed("INC-1014", "Software License Expired - AutoCAD",
                "AutoCAD license expired for Engineering department. 10 workstations affected.",
                emp3, catSoftware, null, Impact.LOW, Urgency.LOW, 10,
                new PriorityScore(1.0, "Low impact and urgency", Priority.P4),
                Priority.P4, Status.NEW, null,
                now.plusHours(4), now.plusHours(24),
                SlaStatus.ON_TRACK, SlaStatus.ON_TRACK,
                null, EscalationLevel.TECHNICIAN,
                null, null, null, null, null, null, null);

        Incident inc15 = Incident.forSeed("INC-1015", "Network Switch Failure - Building B",
                "Core switch in Building B MDF failed. All Building B users without network access.",
                emp1, catNetwork, null, Impact.CRITICAL, Urgency.CRITICAL, 150,
                new PriorityScore(4.0, "Critical infrastructure failure", Priority.P1),
                Priority.P1, Status.ASSIGNED, tech1,
                now.plusMinutes(5), now.plusMinutes(30),
                SlaStatus.ON_TRACK, SlaStatus.ON_TRACK,
                null, EscalationLevel.TECHNICIAN,
                null, null, null, null, null, null, null);

        incidentRepository.saveAll(List.of(inc1, inc2, inc3, inc4, inc5, inc6, inc7, inc8,
                inc9, inc10, inc11, inc12, inc13, inc14, inc15));

        // Update technician workloads based on active assignments
        tech1.setMaxWorkload(6); // assigned: inc7, inc10, inc13, inc15 = 4 active
        tech2.setMaxWorkload(5); // assigned: inc6 = 1 active (inc11 resolved)
        tech3.setMaxWorkload(4); // assigned: inc12 = 1 active

        userRepository.saveAll(List.of(tech1, tech2, tech3));

        log.info("Created 15 seed incidents (5 closed + 10 active)");
        log.info("=== Database seeding complete ===");
    }
}
