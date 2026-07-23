package org.learn.currencyexchanger.auth.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.learn.currencyexchanger.auth.domain.exception.InvalidPasswordException;
import org.learn.currencyexchanger.user.application.exception.UsernameAlreadyUsedException;
import org.learn.currencyexchanger.user.domain.User;
import org.learn.currencyexchanger.user.domain.UserRepository;
import org.learn.currencyexchanger.user.domain.UserRole;
import org.learn.currencyexchanger.user.domain.UserStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthServiceTest {

    private static final String RAW_PASSWORD = "correct horse battery staple";
    private static final String ENCODED_PASSWORD = "{test}encoded-password";

    private InMemoryUserRepository userRepository;
    private RecordingPasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = new InMemoryUserRepository();
        passwordEncoder = new RecordingPasswordEncoder();
        authService = new AuthService(userRepository, passwordEncoder);
    }

    @Test
    void shouldRegisterUserWithNormalizedUsernameAndEncodedPassword() {
        RegistrationResult result = authService.register(
                "   John.DOE   ",
                RAW_PASSWORD
        );

        User storedUser = userRepository.findById(result.userId()).orElseThrow();

        assertAll(
                () -> assertEquals("john.doe", result.username()),
                () -> assertEquals(UserRole.USER, result.role()),
                () -> assertEquals(UserStatus.ACTIVE, result.status()),
                () -> assertEquals("john.doe", storedUser.getUsername()),
                () -> assertEquals(ENCODED_PASSWORD, storedUser.getPasswordHash()),
                () -> assertNotEquals(RAW_PASSWORD, storedUser.getPasswordHash()),
                () -> assertEquals(RAW_PASSWORD, passwordEncoder.lastRawPassword()),
                () -> assertEquals(1, passwordEncoder.encodeCalls()),
                () -> assertEquals(1, userRepository.saveCalls())
        );
    }

    @Test
    void shouldRejectAlreadyUsedUsernameWithoutEncodingPassword() {
        userRepository.addExisting(
                User.register("john.doe", ENCODED_PASSWORD)
        );

        assertThrows(
                UsernameAlreadyUsedException.class,
                () -> authService.register("   JOHN.DOE   ", RAW_PASSWORD)
        );

        assertAll(
                () -> assertEquals(0, passwordEncoder.encodeCalls()),
                () -> assertEquals(0, userRepository.saveCalls())
        );
    }

    @Test
    void shouldRejectInvalidPasswordWithoutSavingUser() {
        assertThrows(
                InvalidPasswordException.class,
                () -> authService.register("john.doe", "too-short")
        );

        assertAll(
                () -> assertEquals(0, passwordEncoder.encodeCalls()),
                () -> assertEquals(0, userRepository.saveCalls())
        );
    }

    private static final class RecordingPasswordEncoder implements PasswordEncoder {

        private String lastRawPassword;
        private int encodeCalls;

        @Override
        public String encode(CharSequence rawPassword) {
            lastRawPassword = rawPassword.toString();
            encodeCalls++;

            return ENCODED_PASSWORD;
        }

        @Override
        public boolean matches(
                CharSequence rawPassword,
                String encodedPassword
        ) {
            return ENCODED_PASSWORD.equals(encodedPassword);
        }

        String lastRawPassword() {
            return lastRawPassword;
        }

        int encodeCalls() {
            return encodeCalls;
        }
    }

    private static final class InMemoryUserRepository implements UserRepository {

        private final Map<UUID, User> users = new HashMap<>();
        private int saveCalls;

        @Override
        public Optional<User> findById(UUID userId) {
            return Optional.ofNullable(users.get(userId));
        }

        @Override
        public Optional<User> findByUsername(String username) {
            return users.values()
                    .stream()
                    .filter(user -> user.getUsername().equals(username))
                    .findFirst();
        }

        @Override
        public boolean existsByUsername(String username) {
            return findByUsername(username).isPresent();
        }

        @Override
        public User save(User user) {
            users.put(user.getId(), user);
            saveCalls++;

            return user;
        }

        void addExisting(User user) {
            users.put(user.getId(), user);
        }

        int saveCalls() {
            return saveCalls;
        }
    }
}