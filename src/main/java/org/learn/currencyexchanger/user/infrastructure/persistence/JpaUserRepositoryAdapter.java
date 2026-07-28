package org.learn.currencyexchanger.user.infrastructure.persistence;

import org.learn.currencyexchanger.user.application.exception.UsernameAlreadyUsedException;
import org.learn.currencyexchanger.user.application.port.UserRepository;
import org.learn.currencyexchanger.user.domain.User;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

// Dodatkowy kod ktory daje kilka korzysci:
// aplikacja nie zlaezy bezposrednio od jparepo
// testy moga uzywac prostego mocka userrepo
// pozniejsza zmiana sposobu przechowywania nie zmienia serwisu

@Repository
public class JpaUserRepositoryAdapter implements UserRepository {
    private static final String USERNAME_UNIQUE_CONSTRAINT =
            "uk_app_user_username";

    private final SpringDataUserRepository springDataUserRepository;

    public JpaUserRepositoryAdapter(SpringDataUserRepository springDataUserRepository) {
        this.springDataUserRepository = springDataUserRepository;
    }

    private static boolean containsConstraint(
            Throwable exception,
            String expectedConstraint
    ) {
        Throwable current = exception;

        while (current != null) {
            if (current
                    //Bledy intellij z importem, stad wykorzystanie konkretnego miejsca w package
                    instanceof org.hibernate.exception.ConstraintViolationException violation
                    && expectedConstraint.equals(
                    violation.getConstraintName()
            )) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }

    @Override
    public Optional<User> findById(UUID userId) {
        return springDataUserRepository.findById(userId);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return springDataUserRepository.findByUsername(username);
    }

    @Override
    public boolean existsByUsername(String username) {
        return springDataUserRepository.existsByUsername(username);
    }

    @Override
    public User save(User user) {
        try {
            //Flush jest celowy, dzieki niemu ograniczenia bazy sprawdzane sa jeszcze wewnatrz adaptera
            // i mozna przetlumaczyc wyjatek
            return springDataUserRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            if (containsConstraint(
                    exception,
                    USERNAME_UNIQUE_CONSTRAINT
            )) {
                throw new UsernameAlreadyUsedException(exception);
            }
            throw exception;
        }
    }
}
