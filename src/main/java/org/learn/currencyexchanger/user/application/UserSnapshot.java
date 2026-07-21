package org.learn.currencyexchanger.user.application;

import org.learn.currencyexchanger.user.domain.User;
import org.learn.currencyexchanger.user.domain.UserRole;
import org.learn.currencyexchanger.user.domain.UserStatus;

import java.util.UUID;

// TO NIE JEST DTO
// Jest wynikiem przypadku uzycia. controller moze go pozniej przeksztalcic na userresponse

public record UserSnapshot(
        UUID id,
        String username,
        UserRole role,
        UserStatus status
) {
    public static UserSnapshot from(User user) {
        return new UserSnapshot(
                user.getId(),
                user.getUsername(),
                user.getUserRole(),
                user.getStatus()
        );
    }
}
