package org.learn.currencyexchanger.user.api;

// DTO - kontrakt ma zwracać tylko potrzebne dane
// Nigdy nie zwraca danych wrazliwych dla uzytkownika

import org.learn.currencyexchanger.user.domain.UserRole;
import org.learn.currencyexchanger.user.domain.UserStatus;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        UserRole role,
        UserStatus status
) {
}
