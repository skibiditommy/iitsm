package com.iitsm.domain.factory;

import com.iitsm.domain.enums.Role;
import com.iitsm.domain.model.Administrator;
import com.iitsm.domain.model.Employee;
import com.iitsm.domain.model.Technician;
import com.iitsm.domain.model.User;
import org.springframework.stereotype.Component;

/**
 * Factory Pattern: Creates the correct {@link User} subclass based on the requested role.
 * <p>
 * OOP: Polymorphism — the caller works with the abstract {@link User} type,
 * while this factory encapsulates concrete subclass instantiation.
 */
@Component
public class UserFactory {

    /**
     * Creates a User subclass based on the given role.
     *
     * @param email        user email
     * @param passwordHash encoded password
     * @param firstName    first name
     * @param lastName     last name
     * @param role         determines the concrete type (EMPLOYEE, TECHNICIAN, ADMIN)
     * @param department   department (for employees)
     * @param phone        phone number (for employees)
     * @return the appropriate User subclass instance
     */
    public User createUser(String email, String passwordHash, String firstName,
                           String lastName, Role role, String department, String phone) {
        return switch (role) {
            case EMPLOYEE -> new Employee(email, passwordHash, firstName, lastName, department, phone);
            case TECHNICIAN -> new Technician(email, passwordHash, firstName, lastName, 0);
            case ADMIN -> new Administrator(email, passwordHash, firstName, lastName);
        };
    }
}
