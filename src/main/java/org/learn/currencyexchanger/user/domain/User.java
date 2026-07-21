package org.learn.currencyexchanger.user.domain;

import jakarta.persistence.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.Objects;
import java.util.UUID;

// Encja nie ma publicznych setterow. Stan zmienia sie przez metody
// opisujace operacje biznesowe

@Entity
@Table(name = "app_user")
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(
            name = "password_hash",
            nullable = false
    )
    private String passwordHash;

    @Column(
            name = "username",
            nullable = false,
            length = 320
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

    protected User() {}

    public User(UUID id,
                String passwordHash,
                String username,
                UserRole userRole,
                UserStatus status
    ) {
        this.id = Objects.requireNonNull(id);
        this.passwordHash = Objects.requireNonNull(passwordHash);
        this.username = Objects.requireNonNull(username);
        this.userRole = Objects.requireNonNull(userRole);
        this.status = Objects.requireNonNull(status);
    }

    public static User register(String username, String passwordHash) {
        return new User(
                UUID.randomUUID(),
                username,
                passwordHash,
                UserRole.USER,
                UserStatus.ACTIVE
        );
    }

    public void changeUsername(String newUsername) {
        requireNotDisabled();

        if (newUsername.trim().equals(username)) return;
        this.username = newUsername.trim();
    }

    public void changePasswordHash(String newPasswordHash) {
        requireNotDisabled();

        this.passwordHash = requirePasswordHash(newPasswordHash);
    }

    public void unlock() {
        if (status != UserStatus.LOCKED)
            throw new IllegalStateException("Only a locked user can be unlocked");

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
            throw new IllegalStateException("Disabled user cannot be modified");
    }

    private static String requirePasswordHash(String passwordHash) {
        Objects.requireNonNull(passwordHash, "Password hash cannot be null");

        if (passwordHash.isBlank()) throw new IllegalArgumentException("Password hash cannot be blank");

        return passwordHash;
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
