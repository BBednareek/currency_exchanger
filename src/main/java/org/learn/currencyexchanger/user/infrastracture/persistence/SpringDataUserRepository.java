package org.learn.currencyexchanger.user.infrastracture.persistence;

import org.learn.currencyexchanger.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataUserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);
    boolean exsistsByUsername(String username);
}
