package org.learn.currencyexchanger.auth.api;

import org.learn.currencyexchanger.user.domain.UserRole;
import org.learn.currencyexchanger.user.domain.UserStatus;

import java.util.UUID;

public record RegistrationResponse(
        UUID id,
        String username,
        UserRole role,
        UserStatus status
) {
}
