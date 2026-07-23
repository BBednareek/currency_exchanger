package org.learn.currencyexchanger.auth.api;

import org.learn.currencyexchanger.auth.application.RegistrationResult;

public final class AuthApiMapper {
    private AuthApiMapper() {

    }

    public static RegistrationResponse toResponse(
            RegistrationResult result
    ) {
        return new RegistrationResponse(
                result.userId(),
                result.username(),
                result.role(),
                result.status()
        );
    }
}
