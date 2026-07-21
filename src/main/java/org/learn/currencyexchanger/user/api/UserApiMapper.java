package org.learn.currencyexchanger.user.api;

import org.learn.currencyexchanger.user.application.UserSnapshot;
import org.springframework.stereotype.Component;

// Mapuje Encje na utworzone DTO dla bezpieczenstwa danych

@Component
public final class UserApiMapper {

    private UserApiMapper() {}

    public static UserResponse toResponse(UserSnapshot snapshot) {
        return new UserResponse(
                snapshot.id(),
                snapshot.username(),
                snapshot.role(),
                snapshot.status()
        );
    }
}
