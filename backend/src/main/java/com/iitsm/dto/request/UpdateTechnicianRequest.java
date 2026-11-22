package com.iitsm.dto.request;

import java.util.List;

public record UpdateTechnicianRequest(
        Boolean available,
        Integer maxWorkload,
        Integer yearsOfExperience,
        List<Long> skillIds
) {}
