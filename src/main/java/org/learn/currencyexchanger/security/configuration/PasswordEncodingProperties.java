package org.learn.currencyexchanger.security.configuration;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "security.password")
public record PasswordEncodingProperties(
        @Min(
                value = 4,
                message = "BCrypt strength cannot be lower than 4")
        @Max(
                value = 31,
                message = "BCrypt strength cannot be greater than 31"
        )
        int bcryptStrength
) {
}
