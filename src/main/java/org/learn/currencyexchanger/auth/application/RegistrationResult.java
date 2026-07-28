package org.learn.currencyexchanger.auth.application;

import org.learn.currencyexchanger.user.domain.User;
import org.learn.currencyexchanger.user.domain.UserRole;
import org.learn.currencyexchanger.user.domain.UserStatus;

import java.util.UUID;

public record RegistrationResult(
        UUID userId,
        String username,
        UserRole role,
        UserStatus status
) {
    public static RegistrationResult from(User user) {
        return new RegistrationResult(
                user.getId(),
                user.getUsername(),
                user.getUserRole(),
                user.getStatus()
        );
    }
}
