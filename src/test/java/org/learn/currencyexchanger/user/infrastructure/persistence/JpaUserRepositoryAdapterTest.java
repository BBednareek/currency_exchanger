package org.learn.currencyexchanger.user.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.OptimisticLockException;
import org.flywaydb.core.internal.database.base.TestContainersDatabaseType;
import org.junit.jupiter.api.Test;
import org.learn.currencyexchanger.TestcontainersConfiguration;
import org.learn.currencyexchanger.user.domain.User;
import org.learn.currencyexchanger.user.domain.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import({
        TestcontainersConfiguration.class,
        JpaUserRepositoryAdapter.class
})
class JpaUserRepositoryAdapterTest {
    private static final String PASSWORD_HASH = "{bcrypt}password-hash";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SpringDataUserRepository springDataUserRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

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
    void shouldEnforceUniqueUsernameInDatabase() {
        springDataUserRepository.saveAndFlush(
                User.register(
                        "john.doe",
                        PASSWORD_HASH
                )
        );

        User duplicate = User.register(
                "   JOHN.DOE    ",
                PASSWORD_HASH
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> springDataUserRepository.saveAndFlush(duplicate)
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

            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    secondTransaction::commit
            );

            assertTrue(
                    hasCause(exception, OptimisticLockException.class),
                    "Expected OptimisticLockException in the cause chain"
            );
        } finally {
            if (firstTransaction.isActive()) firstTransaction.rollback();
            if (secondTransaction.isActive()) secondTransaction.rollback();

            firstEntityManager.close();
            secondEntityManager.close();

            springDataUserRepository.deleteById(savedUser.getId());
        }
    }

    private static boolean hasCause(
            Throwable throwable,
            Class<? extends Throwable> expectedType
    ) {
        Throwable current = throwable;

        while (current != null) {
            if (expectedType.isInstance(current))
                return true;
        current = current.getCause();
    }
    return false;
    }

}