package org.learn.currencyexchanger.auth.application;

import org.learn.currencyexchanger.auth.domain.PasswordPolicy;
import org.learn.currencyexchanger.user.application.exception.UsernameAlreadyUsedException;
import org.learn.currencyexchanger.user.domain.User;
import org.learn.currencyexchanger.user.domain.UserRepository;
import org.learn.currencyexchanger.user.domain.UsernamePolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public RegistrationResult register(String username, String rawPassword) {
        String normalizedUsername = UsernamePolicy.normalize(username);
        PasswordPolicy.validate(rawPassword);

        ensureUsernameIsAvailable(normalizedUsername);

        String passwordHash = passwordEncoder.encode(rawPassword);
        User user = User.register(normalizedUsername, passwordHash);
        User savedUser = userRepository.save(user);

        return RegistrationResult.from(savedUser);
    }

    private void ensureUsernameIsAvailable(String username) {
        if (userRepository.existsByUsername(username)) {
            throw new UsernameAlreadyUsedException();
        }
    }
}
