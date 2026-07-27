package org.learn.currencyexchanger.common.api.problem;

import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;
import java.util.Objects;

@Component
public final class ApiProblemFactory {

    private static final String CODE_PROPERTY = "code";
    private static final String VIOLATIONS_PROPERTY = "violations";

    public ProblemDetail create(
            ApiProblemCode code,
            URI instance
    ) {
        return create(
                code,
                code.defaultDetail(),
                instance
        );
    }

    public ProblemDetail create(
            ApiProblemCode code,
            String detail,
            URI instance
    ) {
        Objects.requireNonNull(code, "Problem code must not be null");
        Objects.requireNonNull(detail, "Problem detail must not be null");
        Objects.requireNonNull(instance, "Problem instance must not be null");

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                code.status(),
                detail
        );

        problem.setType(code.type());
        problem.setTitle(code.title());
        problem.setInstance(instance);
        problem.setProperty(CODE_PROPERTY, code.name());

        return problem;
    }

    public ProblemDetail createValidationProblem(
            List<ValidationViolation> violations,
            URI instance
    ) {
        Objects.requireNonNull(
                violations,
                "Validation violations must not be null"
        );

        ProblemDetail problem = create(
                ApiProblemCode.VALIDATION_FAILED,
                instance
        );

        problem.setProperty(
                VIOLATIONS_PROPERTY,
                List.copyOf(violations)
        );

        return problem;
    }
}
