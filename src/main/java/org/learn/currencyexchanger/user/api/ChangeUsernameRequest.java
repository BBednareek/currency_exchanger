package org.learn.currencyexchanger.user.api;

import jakarta.validation.constraints.NotBlank;
import org.learn.currencyexchanger.user.api.validation.ValidUsername;

public record ChangeUsernameRequest(
        @NotBlank
        @ValidUsername
        String username
) {
}
