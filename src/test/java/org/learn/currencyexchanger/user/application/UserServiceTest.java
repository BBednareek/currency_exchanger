package org.learn.currencyexchanger.user.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.learn.currencyexchanger.user.application.exception.UserNotFoundException;
import org.learn.currencyexchanger.user.application.exception.UsernameAlreadyUsedException;
import org.learn.currencyexchanger.user.domain.User;
import org.learn.currencyexchanger.user.domain.UserRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceTest {
    private InMemoryUserRepository userRepository;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = new InMemoryUserRepository();
        userService = new UserService(userRepository);
    }

    @Test
    void shouldChangeAndNormalizeUsername() {
        User user = userRepository.save(
                User.register(
                        "current.user",
                        "{bcrypt}password-hash"
                )
        );

        UserSnapshot result = userService.changeUsername(
                user.getId(),
                "   NEW.User    "
        );

        assertEquals("new.user", result.username());
    }

    @Test
    void shouldRejectAlreadyUsedUsername() {
        User user = userRepository.save(
                User.register(
                        "current.user",
                        "{bcrypt}password-hash"
                )
        );

        userRepository.save(
                User.register(
                        "existing.user",
                        "{bcryp}password-hash"
                )
        );

        assertThrows(UsernameAlreadyUsedException.class,
                () -> userService.changeUsername(
                        user.getId(),
                        "   EXISTING.User   "));
    }

    @Test
    void shouldThrowWhenUserDoesNotExist() {
        UUID unknownUserId = UUID.randomUUID();
        assertThrows(UserNotFoundException.class, () -> userService.getUser(unknownUserId));
    }

    private static final class InMemoryUserRepository implements UserRepository {
        private final Map<UUID, User> users = new HashMap<>();

        @Override
        public Optional<User> findById(UUID userId) {
            return Optional.ofNullable(users.get(userId));
        }

        @Override
        public Optional<User> findByUsername(String username) {
            return users.
                    values().
                    stream().
                    filter(user -> user.getUsername().equals(username)).
                    findFirst();
        }

        @Override
        public boolean existsByUsername(String username) {
            return findByUsername(username).isPresent();
        }

        @Override
        public User save(User user) {
            users.put(user.getId(), user);
            return user;
        }
    }
}