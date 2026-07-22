package org.learn.currencyexchanger.user.infrastructure.persistence;

import org.learn.currencyexchanger.user.domain.User;
import org.learn.currencyexchanger.user.domain.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

// Dodatkowy kod ktory daje kilka korzysci:
// aplikacja nie zlaezy bezposrednio od jparepo
// testy moga uzywac prostego mocka userrepo
// pozniejsza zmiana sposobu przechowywania nie zmienia serwisu

@Repository
public class JpaUserRepositoryAdapter implements UserRepository {
    private final SpringDataUserRepository springDataUserRepository;

    public JpaUserRepositoryAdapter(SpringDataUserRepository springDataUserRepository) {
        this.springDataUserRepository = springDataUserRepository;
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
        return springDataUserRepository.save(user);
    }
}
