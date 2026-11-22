package com.iitsm.service;

import com.iitsm.domain.exception.UserNotFoundException;
import com.iitsm.domain.model.Skill;
import com.iitsm.domain.model.Technician;
import com.iitsm.domain.model.User;
import com.iitsm.dto.request.UpdateTechnicianRequest;
import com.iitsm.dto.response.*;
import com.iitsm.repository.SkillRepository;
import com.iitsm.repository.TechnicianRepository;
import com.iitsm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TechnicianRepository technicianRepository;
    private final SkillRepository skillRepository;

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());
    }

    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
        return toUserResponse(user);
    }

    public List<TechnicianResponse> getAllTechnicians() {
        return technicianRepository.findAll().stream()
                .map(this::toTechnicianResponse)
                .collect(Collectors.toList());
    }

    public TechnicianResponse getTechnicianById(Long id) {
        Technician tech = technicianRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Technician not found with id: " + id));
        return toTechnicianResponse(tech);
    }

    @Transactional
    public TechnicianResponse updateTechnician(Long id, UpdateTechnicianRequest request) {
        Technician tech = technicianRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Technician not found with id: " + id));

        if (request.available() != null) tech.setAvailable(request.available());
        if (request.maxWorkload() != null) tech.setMaxWorkload(request.maxWorkload());
        if (request.yearsOfExperience() != null) tech.setYearsOfExperience(request.yearsOfExperience());
        if (request.skillIds() != null) {
            tech.getSkills().clear();
            for (Long skillId : request.skillIds()) {
                skillRepository.findById(skillId).ifPresent(tech::addSkill);
            }
        }

        technicianRepository.save(tech);
        return toTechnicianResponse(tech);
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFirstName(),
                user.getLastName(), user.getRole().name(), user.isActive(), user.getFullName());
    }

    public TechnicianResponse toTechnicianResponse(Technician tech) {
        List<SkillResponse> skills = tech.getSkills().stream()
                .map(s -> new SkillResponse(s.getId(), s.getName(),
                        s.getRelatedCategory() != null ? s.getRelatedCategory().getName() : null))
                .collect(Collectors.toList());
        return new TechnicianResponse(tech.getId(), tech.getEmail(), tech.getFirstName(),
                tech.getLastName(), tech.getCurrentWorkload(), tech.getMaxWorkload(),
                tech.getYearsOfExperience(), tech.isAvailable(), skills);
    }
}
