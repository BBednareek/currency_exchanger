package org.learn.currencyexchanger.user.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.RollbackException;
import org.junit.jupiter.api.Test;
import org.learn.currencyexchanger.TestcontainersConfiguration;
import org.learn.currencyexchanger.user.application.exception.UsernameAlreadyUsedException;
import org.learn.currencyexchanger.user.application.port.AccountStatusReader;
import org.learn.currencyexchanger.user.application.port.UserRepository;
import org.learn.currencyexchanger.user.domain.User;
import org.learn.currencyexchanger.user.domain.UserStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Import({
        TestcontainersConfiguration.class,
        JpaUserRepositoryAdapter.class
})
class JpaUserRepositoryAdapterTest {
    private static final String PASSWORD_HASH = "{bcrypt}password-hash";

    @Autowired
    private AccountStatusReader accountStatusReader;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SpringDataUserRepository springDataUserRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldSaveAndFindUserById() {
        User savedUser = userRepository.save(
                User.register(
                        "john.doe",
                        PASSWORD_HASH
                )
        );

        entityManager.flush();
        entityManager.clear();

        Optional<User> result = userRepository.findById(savedUser.getId());

        assertTrue(result.isPresent());
        assertEquals(savedUser.getId(), result.orElseThrow().getId());
        assertEquals("john.doe", result.orElseThrow().getUsername());
    }

    @Test
    void shouldFindUserByNormalizedUsername() {
        User savedUser = userRepository.save(
                User.register(
                        "   John.DOE    ",
                        PASSWORD_HASH
                )
        );

        entityManager.flush();
        entityManager.clear();

        Optional<User> result = userRepository.findByUsername("john.doe");

        assertTrue(result.isPresent());
        assertEquals(savedUser.getId(), result.orElseThrow().getId());
    }

    @Test
    void shouldReportWhetherUsernameExists() {
        userRepository.save(
                User.register(
                        "john.doe",
                        PASSWORD_HASH
                )
        );

        entityManager.flush();

        assertTrue(userRepository.existsByUsername("john.doe"));
        assertFalse(userRepository.existsByUsername("unknown.user"));
    }

    @Test
    void shouldReportActiveAccountAsActive() {
        User user = userRepository.save(
                User.register(
                        "active.user",
                        PASSWORD_HASH
                )
        );

        assertTrue(accountStatusReader.isActive(user.getId()));
    }

    @Test
    void shouldReportLockedAccountAsInactive() {
        User user = User.register(
                "locked.user",
                PASSWORD_HASH
        );
        user.lock();

        userRepository.save(user);

        assertFalse(accountStatusReader.isActive(user.getId()));
    }

    @Test
    void shouldReportDisabledAccountAsInactive() {
        User user = User.register(
                "disabled.status.user",
                PASSWORD_HASH
        );
        user.disable(
                Instant.parse("2026-07-27T10:15:30Z")
        );

        userRepository.save(user);

        assertFalse(accountStatusReader.isActive(user.getId()));
    }

    @Test
    void shouldReportMissingAccountAsInactive() {
        assertFalse(
                accountStatusReader.isActive(UUID.randomUUID())
        );
    }

    @Test
    void shouldPersistAccountDisableTimestamp() {
        Instant disabledAt =
                Instant.parse("2026-07-27T10:15:30Z");
        User user = User.register(
                "disabled.user",
                PASSWORD_HASH
        );
        user.disable(disabledAt);

        User savedUser =
                springDataUserRepository.saveAndFlush(user);

        entityManager.clear();

        User reloadedUser = springDataUserRepository
                .findById(savedUser.getId())
                .orElseThrow();

        assertEquals(
                UserStatus.DISABLED,
                reloadedUser.getStatus()
        );
        assertEquals(
                disabledAt,
                reloadedUser.getDisabledAt()
        );
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldGenerateAndUpdateAuditTimestamps() {
        User savedUser =
                springDataUserRepository.saveAndFlush(
                        User.register(
                                "audited.user",
                                PASSWORD_HASH
                        )
                );

        Instant createdAt = savedUser.getCreatedAt();
        Instant initialUpdatedAt = savedUser.getUpdatedAt();

        assertNotNull(createdAt);
        assertNotNull(initialUpdatedAt);
        assertFalse(initialUpdatedAt.isBefore(createdAt));

        User userToUpdate = springDataUserRepository
                .findById(savedUser.getId())
                .orElseThrow();

        userToUpdate.changeUsername("audited.user.changed");

        User updatedUser =
                springDataUserRepository.saveAndFlush(
                        userToUpdate
                );

        assertEquals(
                createdAt,
                updatedUser.getCreatedAt()
        );
        assertNotNull(updatedUser.getUpdatedAt());
        assertTrue(
                updatedUser.getUpdatedAt()
                        .isAfter(initialUpdatedAt)
        );
    }

    @Test
    void shouldTranslateUsernameUniqueConstraint() {
        springDataUserRepository.saveAndFlush(
                User.register(
                        "john.doe",
                        PASSWORD_HASH
                )
        );

        User duplicate = User.register(
                "   JOHN.DOE   ",
                PASSWORD_HASH
        );

        UsernameAlreadyUsedException exception =
                assertThrows(
                        UsernameAlreadyUsedException.class,
                        () -> userRepository.save(duplicate)
                );

        assertInstanceOf(
                DataIntegrityViolationException.class,
                exception.getCause()
        );
    }

    @Test
    void shouldEnforceNormalizedUsernameFormatInDatabase() {
        assertThrows(
                DataIntegrityViolationException.class,
                () -> jdbcTemplate.update(
                        """
                                INSERT INTO app_user (
                                    id,
                                    username,
                                    password_hash,
                                    role,
                                    status,
                                    version
                                )
                                VALUES (?, ?, ?, ?, ?, ?)
                                """,
                        UUID.randomUUID(),
                        "Invalid Username",
                        PASSWORD_HASH,
                        "USER",
                        "ACTIVE",
                        0L
                )
        );
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldRejectStaleConcurrentUpdate() {
        User savedUser = springDataUserRepository.saveAndFlush(
                User.register(
                        "concurrent.user",
                        PASSWORD_HASH
                )
        );

        EntityManager firstEntityManager = entityManagerFactory.createEntityManager();
        EntityManager secondEntityManager = entityManagerFactory.createEntityManager();

        EntityTransaction firstTransaction = firstEntityManager.getTransaction();
        EntityTransaction secondTransaction = secondEntityManager.getTransaction();

        try {
            firstTransaction.begin();
            secondTransaction.begin();

            User firstCopy = firstEntityManager.find(
                    User.class,
                    savedUser.getId()
            );

            User secondCopy = secondEntityManager.find(
                    User.class,
                    savedUser.getId()
            );

            assertNotNull(firstCopy);
            assertNotNull(secondCopy);

            firstCopy.changeUsername("first.update");
            firstTransaction.commit();

            secondCopy.changeUsername("second.update");

            assertThrows(
                    RollbackException.class,
                    secondTransaction::commit
            );

            try (EntityManager verificationEntityManager = entityManagerFactory.createEntityManager()) {
                User persistedUser =
                        verificationEntityManager.find(
                                User.class,
                                savedUser.getId()
                        );

                assertNotNull(persistedUser);
                assertEquals(
                        "first.update",
                        persistedUser.getUsername()
                );
            }
        } finally {
            if (firstTransaction.isActive()) firstTransaction.rollback();
            if (secondTransaction.isActive()) secondTransaction.rollback();

            firstEntityManager.close();
            secondEntityManager.close();

            springDataUserRepository.deleteById(savedUser.getId());
        }
    }

}
