package org.learn.currencyexchanger.common.api.problem;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiProblemFactoryTest {

    private static final URI INSTANCE =
            URI.create("/api/users/me");

    private final ApiProblemFactory factory =
            new ApiProblemFactory();

    @ParameterizedTest
    @EnumSource(ApiProblemCode.class)
    void shouldCreateProblemFromEveryRegisteredCode(
            ApiProblemCode code
    ) {
        ProblemDetail problem = factory.create(
                code,
                INSTANCE
        );

        assertAll(
                () -> assertEquals(
                        code.type(),
                        problem.getType()
                ),
                () -> assertEquals(
                        code.title(),
                        problem.getTitle()
                ),
                () -> assertEquals(
                        code.status().value(),
                        problem.getStatus()
                ),
                () -> assertEquals(
                        code.defaultDetail(),
                        problem.getDetail()
                ),
                () -> assertEquals(
                        INSTANCE,
                        problem.getInstance()
                ),
                () -> assertEquals(
                        code.name(),
                        problem.getProperties().get("code")
                )
        );
    }

    @Test
    void shouldUseCustomDetail() {
        ProblemDetail problem = factory.create(
                ApiProblemCode.INVALID_USERNAME,
                "Username contains unsupported characters",
                INSTANCE
        );

        assertEquals(
                "Username contains unsupported characters",
                problem.getDetail()
        );
    }

    @Test
    void shouldDefensivelyCopyValidationViolations() {
        List<ValidationViolation> violations =
                new ArrayList<>();

        ValidationViolation violation =
                new ValidationViolation(
                        "username",
                        "Username is invalid"
                );

        violations.add(violation);

        ProblemDetail problem =
                factory.createValidationProblem(
                        violations,
                        INSTANCE
                );

        violations.clear();

        assertEquals(
                List.of(violation),
                problem.getProperties().get("violations")
        );
    }
}
