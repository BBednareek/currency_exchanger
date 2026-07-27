package org.learn.currencyexchanger.auth.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.learn.currencyexchanger.user.api.validation.ValidUsername;

public record RegisterRequest(
        @NotBlank(message = "Username is required")
        @ValidUsername
        String username,

        //nie dodano @Size ze wzgledu na to, ze polityyka uwzgledniajaca unicode i bajty utf-8 liczy znaki inaczej
        @NotNull(message = "Password is required")
        String password

) {

    @Override
    public String toString() {
        return "RegisterRequest[username=%s, password=<redacted>]"
                .formatted(username);
    }
}
