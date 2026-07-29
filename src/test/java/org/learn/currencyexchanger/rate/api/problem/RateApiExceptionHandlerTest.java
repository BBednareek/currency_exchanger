package org.learn.currencyexchanger.rate.api.problem;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.learn.currencyexchanger.common.api.problem.ApiProblemCode;
import org.learn.currencyexchanger.common.api.problem.ApiProblemFactory;
import org.learn.currencyexchanger.rate.application.exception.InvalidRateProviderResponseException;
import org.learn.currencyexchanger.rate.application.exception.RateProviderUnavailableException;
import org.learn.currencyexchanger.rate.application.exception.ReferenceRateNotFoundException;
import org.learn.currencyexchanger.rate.application.exception.UnsupportedCurrencyException;
import org.learn.currencyexchanger.rate.domain.CurrencyPair;
import org.learn.currencyexchanger.rate.domain.exception.InvalidCurrencyCodeException;
import org.learn.currencyexchanger.rate.domain.exception.InvalidCurrencyPairException;
import org.learn.currencyexchanger.rate.domain.exception.InvalidMoneyAmountException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers =
                RateApiExceptionHandlerTest.TestController.class
)
@AutoConfigureMockMvc(addFilters = false)
@Import({
        RateApiExceptionHandler.class,
        ApiProblemFactory.class,
        RateApiExceptionHandlerTest.TestController.class
})
class RateApiExceptionHandlerTest {

    private static final String BASE_PATH =
            "/api/test/problems/rate";

    private static final CurrencyPair CURRENCY_PAIR =
            CurrencyPair.of("USD", "PLN");

    private final MockMvc mockMvc;

    @Autowired
    RateApiExceptionHandlerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    private static Stream<Arguments> mappedProblems() {
        return Stream.of(
                Arguments.of(
                        "invalid-money-amount",
                        ApiProblemCode.INVALID_MONEY_AMOUNT,
                        "Money amount must be greater than zero"
                ),
                Arguments.of(
                        "invalid-currency-code",
                        ApiProblemCode.INVALID_CURRENCY_CODE,
                        "Currency code must contain exactly "
                                + "three ASCII letters"
                ),
                Arguments.of(
                        "invalid-currency-pair",
                        ApiProblemCode.INVALID_CURRENCY_PAIR,
                        "Base and quote currencies must be different"
                ),
                Arguments.of(
                        "unsupported-currency",
                        ApiProblemCode.UNSUPPORTED_CURRENCY,
                        "Currency pair is not supported: USD/PLN"
                ),
                Arguments.of(
                        "reference-rate-not-found",
                        ApiProblemCode.REFERENCE_RATE_NOT_FOUND,
                        "Reference rate was not found for: USD/PLN"
                ),
                Arguments.of(
                        "invalid-provider-response",
                        ApiProblemCode.INVALID_RATE_PROVIDER_RESPONSE,
                        ApiProblemCode.INVALID_RATE_PROVIDER_RESPONSE
                                .defaultDetail()
                ),
                Arguments.of(
                        "provider-unavailable",
                        ApiProblemCode.RATE_PROVIDER_UNAVAILABLE,
                        ApiProblemCode.RATE_PROVIDER_UNAVAILABLE
                                .defaultDetail()
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("mappedProblems")
    void shouldMapRateExceptionToProblemDetail(
            String caseName,
            ApiProblemCode expectedCode,
            String expectedDetail
    ) throws Exception {
        String path = BASE_PATH + "/" + caseName;

        mockMvc.perform(
                        get(path)
                                .accept(
                                        MediaType.APPLICATION_PROBLEM_JSON
                                )
                )
                .andExpect(status().is(
                        expectedCode.status().value()
                ))
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(jsonPath("$.type").value(
                        expectedCode.type().toString()
                ))
                .andExpect(jsonPath("$.title").value(
                        expectedCode.title()
                ))
                .andExpect(jsonPath("$.status").value(
                        expectedCode.status().value()
                ))
                .andExpect(jsonPath("$.detail").value(
                        expectedDetail
                ))
                .andExpect(jsonPath("$.instance").value(path))
                .andExpect(jsonPath("$.code").value(
                        expectedCode.name()
                ))
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @RestController
    @RequestMapping(BASE_PATH)
    public static class TestController {

        @GetMapping("/{caseName}")
        void throwMappedException(
                @PathVariable String caseName
        ) {
            throw switch (caseName) {
                case "invalid-money-amount" -> new InvalidMoneyAmountException();

                case "invalid-currency-code" -> new InvalidCurrencyCodeException();

                case "invalid-currency-pair" -> new InvalidCurrencyPairException();

                case "unsupported-currency" -> new UnsupportedCurrencyException(
                        CURRENCY_PAIR
                );

                case "reference-rate-not-found" -> new ReferenceRateNotFoundException(
                        CURRENCY_PAIR
                );

                case "invalid-provider-response" -> new InvalidRateProviderResponseException(
                        "Sensitive provider payload"
                );

                case "provider-unavailable" -> new RateProviderUnavailableException(
                        new IllegalStateException(
                                "Sensitive connection details"
                        )
                );

                default -> new IllegalArgumentException(
                        "Unknown test case: " + caseName
                );
            };
        }
    }
}
