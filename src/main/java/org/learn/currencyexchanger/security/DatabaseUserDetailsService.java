package org.learn.currencyexchanger.security;

import org.jspecify.annotations.NullMarked;
import org.learn.currencyexchanger.user.domain.exception.InvalidUsernameException;
import org.learn.currencyexchanger.user.domain.UserRepository;
import org.learn.currencyexchanger.user.domain.UsernamePolicy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// Do sesji trafia AppUserPrincipal, a nie encja JPA.

@Service
public class DatabaseUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    private static final String INVALID_CREDENTIALS = "Invalid credentials";

    public DatabaseUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @NullMarked
    @Override
    public UserDetails loadUserByUsername(String username) {
        try {
            String normalizedUsername = UsernamePolicy.normalize(username);

            return userRepository.findByUsername(normalizedUsername)
                    .map(AppUserPrincipal::from)
                    .orElseThrow(this::invalidCredentials);
        } catch (InvalidUsernameException exception) {
            throw invalidCredentials();
        }
    }

    private UsernameNotFoundException invalidCredentials() {
        return new UsernameNotFoundException(INVALID_CREDENTIALS);
    }
}
