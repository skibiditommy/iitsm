package com.iitsm;

import com.iitsm.domain.model.ITService;
import com.iitsm.repository.ITServiceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:service-impact-tests")
@AutoConfigureMockMvc
class ServiceImpactTests {
    @Autowired MockMvc mvc;
    @Autowired ITServiceRepository services;

    @Test
    void pathsExcludeTheSourceAndCountEachDestinationOnceInADiamondWithACycle() {
        ITService source = new ITService("Source", "", 5);
        ITService left = new ITService("Left", "", 3);
        ITService right = new ITService("Right", "", 3);
        ITService end = new ITService("End", "", 2);
        source.getDownstreamDependencies().addAll(List.of(right, left));
        left.getDownstreamDependencies().add(end);
        right.getDownstreamDependencies().add(end);
        end.getDownstreamDependencies().add(source);

        assertThat(source.getImpactPaths()).containsExactly(
                List.of(source, left), List.of(source, right), List.of(source, left, end));
        assertThat(source.getBlastRadius()).containsExactlyInAnyOrder(left, right, end);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void sharedCausesRankDistinctActiveServicesAndExposeLinkedEvidence() throws Exception {
        mvc.perform(get("/api/analytics/service-impacts")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Auth Service"))
                .andExpect(jsonPath("$[0].affectedDependentCount").value(4))
                .andExpect(jsonPath("$[0].dependents[*].path", hasItem(List.of("Auth Service", "Email Service", "CRM Application"))))
                .andExpect(jsonPath("$[0].dependents[*].activeTickets[*].incidentId", hasItem("INC-1006")))
                .andExpect(jsonPath("$[?(@.name == 'Monitoring Platform')].activeTickets[*]").isEmpty());
    }

    @Test
    @WithMockUser(username = "employee@iitsm.org", roles = "EMPLOYEE")
    void employeeCanReportAgainstSeededServicesButCannotReadAdminAnalysis() throws Exception {
        mvc.perform(get("/api/analytics/service-impacts")).andExpect(status().isForbidden());
        long serviceId = services.findByName("Email Service").orElseThrow().getId();
        String body = """
                {"title":"Mail search stalled","description":"Search stops loading","impact":"LOW",
                 "urgency":"LOW","affectedUsersCount":1,"affectedServiceId":%d}
                """.formatted(serviceId);
        mvc.perform(post("/api/incidents").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.incidentId").value("INC-1016"))
                .andExpect(jsonPath("$.affectedService").value("Email Service"));
        mvc.perform(post("/api/incidents").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.incidentId").value("INC-1017"));
        mvc.perform(get("/api/incidents/my")).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].incidentId", hasItem("INC-1016")));
    }
}
