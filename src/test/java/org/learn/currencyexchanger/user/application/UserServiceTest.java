package org.learn.currencyexchanger.user.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.learn.currencyexchanger.user.application.exception.UserNotFoundException;
import org.learn.currencyexchanger.user.application.exception.UsernameAlreadyUsedException;
import org.learn.currencyexchanger.user.domain.User;
import org.learn.currencyexchanger.user.domain.UserRepository;
import org.learn.currencyexchanger.user.domain.UserStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserServiceTest {
    private static final String PASSWORD_HASH = "{bcrypt}password-hash";
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
                        PASSWORD_HASH
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
                        PASSWORD_HASH
                )
        );

        userRepository.save(
                User.register(
                        "existing.user",
                        "{bcrypt}password-hash"
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

    @Test
    void shouldReturnCurrentUserWithoutChangingSameUsername() {
        User user = userRepository.save(
                User.register(
                        "john.doe",
                        PASSWORD_HASH
                )
        );

        UserSnapshot result = userService.changeUsername(
                user.getId(),
                "   JOHN.DOE    "
        );

        assertEquals("john.doe", result.username());
    }

    @Test
    void shouldDisableOwnAccount() {
        User user = userRepository.save(
                User.register(
                        "john.doe",
                        PASSWORD_HASH
                )
        );

        userService.disableOwnAccount(user.getId());

        User storedUser = userRepository.findById(user.getId()).orElseThrow();

        assertEquals(UserStatus.DISABLED, storedUser.getStatus());
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