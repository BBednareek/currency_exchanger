package org.learn.currencyexchanger.auth.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.learn.currencyexchanger.user.domain.UsernamePolicy;

public record RegisterRequest(
        @NotBlank(message = "Username is required")
        @Size(
                min = UsernamePolicy.MIN_LENGTH,
                max = UsernamePolicy.MAX_LENGTH,
                message = "Username must contain between 3 and 50 characters"
        )
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
