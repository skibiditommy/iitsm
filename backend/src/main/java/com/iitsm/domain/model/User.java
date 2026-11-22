package com.iitsm.domain.model;

import com.iitsm.domain.enums.Action;
import com.iitsm.domain.enums.Permission;
import com.iitsm.domain.enums.Role;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

/**
 * Abstract base class for all system users (Employee, Technician, Administrator).
 * <p>
 * OOP: Abstraction &amp; Inheritance — Abstract base class with polymorphic behavior.
 * Subclasses define their own dashboard permissions and action authorization rules
 * via {@link #getDashboardPermissions()} and {@link #canPerformAction(Action)}.
 * </p>
 * <p>
 * Uses single-table inheritance: all user types are stored in the {@code users} table,
 * discriminated by the {@code user_type} column.
 * </p>
 */
@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "user_type", discriminatorType = DiscriminatorType.STRING)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    private String firstName;

    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private boolean active = true;

    private String resetToken;

    private LocalDateTime resetTokenExpiry;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    private LocalDateTime createdAt;

    protected User(String email, String passwordHash, String firstName, String lastName, Role role) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public void setResetToken(String resetToken, LocalDateTime expiry) {
        this.resetToken = resetToken;
        this.resetTokenExpiry = expiry;
    }

    public boolean isResetTokenValid(String token) {
        return this.resetToken != null && this.resetToken.equalsIgnoreCase(token)
                && (this.resetTokenExpiry == null || LocalDateTime.now().isBefore(this.resetTokenExpiry));
    }

    public void updatePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
        this.resetToken = null;
        this.resetTokenExpiry = null;
    }

    /**
     * Returns the set of dashboard permissions available to this user type.
     *
     * @return an unmodifiable set of {@link Permission} values
     */
    public abstract Set<Permission> getDashboardPermissions();

    /**
     * Determines whether this user is authorized to perform the given action.
     *
     * @param action the action to check
     * @return {@code true} if the user can perform the action, {@code false} otherwise
     */
    public abstract boolean canPerformAction(Action action);

    /**
     * Returns the user's full name (first + last).
     *
     * @return the full name string
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    public boolean isAccountLocked() {
        return lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now());
    }

    public void recordFailedLogin() {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= 5) {
            this.lockedUntil = LocalDateTime.now().plusMinutes(15);
        }
    }

    public void resetFailedLogins() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
    }

    public LocalDateTime getLockedUntil() {
        return lockedUntil;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    /**
     * Deactivates this user account.
     */
    public void deactivate() {
        this.active = false;
    }

    /**
     * Activates this user account.
     */
    public void activate() {
        this.active = true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id != null && Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + id + ", email='" + email + "', role=" + role + "}";
    }
}
