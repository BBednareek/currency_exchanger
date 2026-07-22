package org.learn.currencyexchanger.user.application;
import org.learn.currencyexchanger.user.application.exception.UserNotFoundException;
import org.learn.currencyexchanger.user.application.exception.UsernameAlreadyUsedException;
import org.learn.currencyexchanger.user.domain.User;
import org.learn.currencyexchanger.user.domain.UserRepository;
import org.learn.currencyexchanger.user.domain.UsernamePolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

// Serwis powinien zawierac operacje dotyczace uzytkownika
// ale nie sam proces logowania, czy rejestracji - to nalezy do innego serwisu (Auth)

@Service
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserSnapshot getUser(UUID userId) {
        return UserSnapshot.from(findUser(userId));
    }

    @Transactional
    public UserSnapshot changeUsername(UUID userId, String newUsername) {
        User user = findUser(userId);
        String normalizedUsername = UsernamePolicy.normalize(newUsername);

        if (user.getUsername().equals(normalizedUsername))
            return UserSnapshot.from(user);

        if (userRepository.existsByUsername(normalizedUsername))
            throw new UsernameAlreadyUsedException();

        user.changeUsername(normalizedUsername);

        User savedUser = userRepository.save(user);

        return UserSnapshot.from(savedUser);
    }

    @Transactional
    public void disableOwnAccount(UUID userId) {
        User user = findUser(userId);

        user.disable();
        userRepository.save(user);
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

}
