package org.learn.currencyexchanger.user.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.learn.currencyexchanger.user.domain.UsernamePolicy;

public record ChangeUsernameRequest(
        @NotBlank
        @Size(
                min = UsernamePolicy.MIN_LENGTH,
                max = UsernamePolicy.MAX_LENGTH,
                message = "Username must containt between 3 and 50 characters"
        )
        String username
) {
}
