package org.learn.currencyexchanger.user.api;

import org.learn.currencyexchanger.user.application.UserSnapshot;

// Mapuje Encje na utworzone DTO dla bezpieczenstwa danych

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
