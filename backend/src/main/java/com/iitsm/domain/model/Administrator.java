package com.iitsm.domain.model;

import com.iitsm.domain.enums.Action;
import com.iitsm.domain.enums.Permission;
import com.iitsm.domain.enums.Role;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Represents a system administrator with full access to all features.
 * <p>
 * Administrators can perform any action and have all dashboard permissions.
 * </p>
 */
@Entity
@DiscriminatorValue("ADMINISTRATOR")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Administrator extends User {

    private String adminLevel = "SENIOR";

    public Administrator(String email, String passwordHash, String firstName, String lastName) {
        super(email, passwordHash, firstName, lastName, Role.ADMIN);
    }

    @Override
    public Set<Permission> getDashboardPermissions() {
        return new LinkedHashSet<>(Arrays.asList(Permission.values()));
    }

    @Override
    public boolean canPerformAction(Action action) {
        return true;
    }
}
