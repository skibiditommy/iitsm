package com.iitsm.domain.model;

import com.iitsm.domain.enums.Action;
import com.iitsm.domain.enums.Permission;
import com.iitsm.domain.enums.Role;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Represents an end-user employee who can create and track incidents.
 * <p>
 * Employees have limited permissions: they can view their own incidents,
 * create new ones, and verify resolutions proposed by technicians.
 * </p>
 */
@Entity
@DiscriminatorValue("EMPLOYEE")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Employee extends User {

    private String department;

    private String phone;

    public Employee(String email, String passwordHash, String firstName, String lastName,
                    String department, String phone) {
        super(email, passwordHash, firstName, lastName, Role.EMPLOYEE);
        this.department = department;
        this.phone = phone;
    }

    @Override
    public Set<Permission> getDashboardPermissions() {
        return Set.of(
                Permission.VIEW_OWN_INCIDENTS,
                Permission.CREATE_INCIDENT,
                Permission.VERIFY_RESOLUTION
        );
    }

    @Override
    public boolean canPerformAction(Action action) {
        return action == Action.CREATE_INCIDENT
                || action == Action.VIEW_INCIDENT
                || action == Action.VERIFY_RESOLUTION;
    }
}
