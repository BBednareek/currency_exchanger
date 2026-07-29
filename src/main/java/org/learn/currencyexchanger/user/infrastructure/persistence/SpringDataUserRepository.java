package org.learn.currencyexchanger.user.infrastructure.persistence;

import org.learn.currencyexchanger.user.domain.User;
import org.learn.currencyexchanger.user.domain.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataUserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByIdAndStatus(
            UUID userId,
            UserStatus status
    );
}
