package org.learn.currencyexchanger.security.configuration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordEncodingConfigurationTest {

    private static final String RAW_PASSWORD =
            "correct-horse-battery-staple";

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withUserConfiguration(
                            PasswordEncodingConfiguration.class
                    );

    @Test
    void shouldBindPropertiesAndCreatePasswordEncoder() {
        contextRunner
                .withPropertyValues(
                        "security.password.bcrypt-strength=4"
                )
                .run(context -> {
                    assertNull(context.getStartupFailure());

                    PasswordEncodingProperties properties =
                            context.getBean(
                                    PasswordEncodingProperties.class
                            );

                    PasswordEncoder passwordEncoder =
                            context.getBean(PasswordEncoder.class);

                    String encodedPassword =
                            passwordEncoder.encode(RAW_PASSWORD);

                    assertEquals(
                            4,
                            properties.bcryptStrength()
                    );

                    assertTrue(
                            encodedPassword.startsWith("{bcrypt}")
                    );

                    assertTrue(
                            passwordEncoder.matches(
                                    RAW_PASSWORD,
                                    encodedPassword
                            )
                    );
                });
    }

    @ParameterizedTest
    @ValueSource(ints = {3, 32})
    void shouldRejectInvalidBcryptStrength(
            int invalidStrength
    ) {
        contextRunner
                .withPropertyValues(
                        "security.password.bcrypt-strength="
                                + invalidStrength
                )
                .run(context ->
                        assertNotNull(
                                context.getStartupFailure()
                        )
                );
    }

    @Test
    void shouldRejectBcryptHashWithoutEncoderIdentifier() {
        contextRunner
                .withPropertyValues(
                        "security.password.bcrypt-strength=4"
                )
                .run(context -> {
                    PasswordEncoder passwordEncoder =
                            context.getBean(PasswordEncoder.class);

                    String hashWithoutIdentifier =
                            new BCryptPasswordEncoder(4)
                                    .encode(RAW_PASSWORD);

                    assertThrows(
                            IllegalArgumentException.class,
                            () -> passwordEncoder.matches(
                                    RAW_PASSWORD,
                                    hashWithoutIdentifier
                            )
                    );
                });
    }
}
