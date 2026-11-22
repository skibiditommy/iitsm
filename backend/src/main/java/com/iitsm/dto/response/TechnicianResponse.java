package com.iitsm.dto.response;

import java.util.List;

public record TechnicianResponse(Long id, String email, String firstName, String lastName,
                                  int currentWorkload, int maxWorkload, int yearsOfExperience,
                                  boolean available, List<SkillResponse> skills) {}
