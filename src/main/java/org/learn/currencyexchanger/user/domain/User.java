package org.learn.currencyexchanger.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.learn.currencyexchanger.user.domain.exception.DisabledUserCannotBeModifiedException;
import org.learn.currencyexchanger.user.domain.exception.UserCannotBeUnlockedException;

import java.util.Objects;
import java.util.UUID;

// Encja nie ma publicznych setterow. Stan zmienia sie przez metody
// opisujace operacje biznesowe

@Entity
@Table(name = "app_user")
public class User {

    private static final int MAX_PASSWORD_HASH_LENGTH = 255;
    @Id
    private UUID id;
    @Column(
            name = "password_hash",
            nullable = false,
            length = 255
    )
    private String passwordHash;
    @Column(
            name = "username",
            nullable = false,
            unique = true,
            length = 50
    )
    private String username;
    @Enumerated(EnumType.STRING)
    @Column(
            name = "role",
            nullable = false,
            length = 15
    )
    private UserRole userRole;
    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 15
    )
    private UserStatus status;
    @Version
    @Column(
            name = "version",
            nullable = false
    )
    private long version;

    protected User() {
    }

    private User(UUID id,
                 String passwordHash,
                 String username,
                 UserRole userRole,
                 UserStatus status
    ) {
        this.id = Objects.requireNonNull(id);
        this.passwordHash = requirePasswordHash(passwordHash);
        this.username = UsernamePolicy.normalize(username);
        this.userRole = Objects.requireNonNull(userRole);
        this.status = Objects.requireNonNull(status);
    }

    public static User register(String username, String passwordHash) {
        return new User(
                UUID.randomUUID(),
                passwordHash,
                username,
                UserRole.USER,
                UserStatus.ACTIVE
        );
    }

    private static String requirePasswordHash(String passwordHash) {
        Objects.requireNonNull(passwordHash, "Password hash cannot be null");

        if (passwordHash.isBlank()) throw new IllegalArgumentException("Password hash cannot be blank");
        if (passwordHash.length() > MAX_PASSWORD_HASH_LENGTH) {
            throw new IllegalArgumentException(
                    "Password hash cannot exceed "
                            + MAX_PASSWORD_HASH_LENGTH
                            + " characters"
            );
        }
        return passwordHash;
    }

    public void changeUsername(String newUsername) {
        requireNotDisabled();

        this.username = UsernamePolicy.normalize(newUsername);
    }

    public void changePasswordHash(String newPasswordHash) {
        requireNotDisabled();

        this.passwordHash = requirePasswordHash(newPasswordHash);
    }

    public void unlock() {
        if (status != UserStatus.LOCKED)
            throw new UserCannotBeUnlockedException(status);

        this.status = UserStatus.ACTIVE;
    }

    public void lock() {
        requireNotDisabled();

        this.status = UserStatus.LOCKED;
    }

    public void disable() {
        this.status = UserStatus.DISABLED;
    }

    private void requireNotDisabled() {
        if (status == UserStatus.DISABLED)
            throw new DisabledUserCannotBeModifiedException();
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserRole getUserRole() {
        return userRole;
    }

    public UserStatus getStatus() {
        return status;
    }

    public long getVersion() {
        return version;
    }
}
