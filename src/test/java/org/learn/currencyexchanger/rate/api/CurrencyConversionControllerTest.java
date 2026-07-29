package org.learn.currencyexchanger.rate.api;

import org.junit.jupiter.api.Test;
import org.learn.currencyexchanger.common.api.problem.ApiExceptionHandler;
import org.learn.currencyexchanger.common.api.problem.ApiProblemFactory;
import org.learn.currencyexchanger.rate.api.problem.RateApiExceptionHandler;
import org.learn.currencyexchanger.rate.application.ConversionSnapshot;
import org.learn.currencyexchanger.rate.application.CurrencyConversionService;
import org.learn.currencyexchanger.rate.domain.Money;
import org.learn.currencyexchanger.rate.domain.exception.InvalidMoneyAmountException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CurrencyConversionController.class)
@Import({
        ApiExceptionHandler.class,
        ApiProblemFactory.class,
        RateApiExceptionHandler.class
})
class CurrencyConversionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CurrencyConversionService conversionService;

    @Test
    void shouldReturnConvertedAmount() throws Exception {
        BigDecimal amount =
                new BigDecimal("100.00");

        ConversionSnapshot snapshot =
                new ConversionSnapshot(
                        Money.of("USD", amount),
                        Money.of(
                                "PLN",
                                new BigDecimal(
                                        "367.21000000"
                                )
                        ),
                        new BigDecimal("3.672100"),
                        LocalDate.of(2026, 7, 28),
                        Instant.parse(
                                "2026-07-28T12:00:00Z"
                        ),
                        false
                );

        when(conversionService.convert(
                "usd",
                "pln",
                amount
        )).thenReturn(snapshot);

        mockMvc.perform(
                        get("/api/conversions")
                                .with(user("john.doe"))
                                .param("base", "usd")
                                .param("quote", "pln")
                                .param("amount", "100.00")
                                .accept(
                                        MediaType.APPLICATION_JSON
                                )
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.source.currency")
                        .value("USD"))
                .andExpect(jsonPath("$.source.amount")
                        .value(100.00))
                .andExpect(jsonPath("$.target.currency")
                        .value("PLN"))
                .andExpect(jsonPath("$.target.amount")
                        .value(367.21000000))
                .andExpect(jsonPath("$.referenceRate")
                        .value(3.672100))
                .andExpect(jsonPath("$.effectiveDate")
                        .value("2026-07-28"))
                .andExpect(jsonPath("$.fetchedAt")
                        .value("2026-07-28T12:00:00Z"))
                .andExpect(jsonPath("$.stale")
                        .value(false));

        verify(conversionService).convert(
                "usd",
                "pln",
                amount
        );
    }

    @Test
    void shouldReturnInvalidMoneyProblemForZeroAmount()
            throws Exception {
        when(conversionService.convert(
                "USD",
                "PLN",
                BigDecimal.ZERO
        )).thenThrow(
                new InvalidMoneyAmountException()
        );

        mockMvc.perform(
                        get("/api/conversions")
                                .with(user("john.doe"))
                                .param("base", "USD")
                                .param("quote", "PLN")
                                .param("amount", "0")
                                .accept(
                                        MediaType.APPLICATION_PROBLEM_JSON
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(jsonPath("$.code")
                        .value("INVALID_MONEY_AMOUNT"))
                .andExpect(jsonPath("$.title")
                        .value("Invalid money amount"))
                .andExpect(jsonPath("$.detail")
                        .value(
                                "Money amount must be greater than zero"
                        ))
                .andExpect(jsonPath("$.instance")
                        .value("/api/conversions"));

        verify(conversionService).convert(
                "USD",
                "PLN",
                BigDecimal.ZERO
        );
    }

    @Test
    void shouldReturnValidationProblemForExcessivePrecision()
            throws Exception {
        mockMvc.perform(
                        get("/api/conversions")
                                .with(user("john.doe"))
                                .param("base", "USD")
                                .param("quote", "PLN")
                                .param(
                                        "amount",
                                        "1.123456789"
                                )
                                .accept(
                                        MediaType.APPLICATION_PROBLEM_JSON
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.violations[0].path")
                        .value("amount"))
                .andExpect(jsonPath("$.violations[0].message")
                        .value(
                                "Amount can contain at most "
                                        + "18 integer and "
                                        + "8 fractional digits"
                        ));

        verifyNoInteractions(conversionService);
    }

    @Test
    void shouldReturnInvalidParameterWhenAmountIsMissing()
            throws Exception {
        mockMvc.perform(
                        get("/api/conversions")
                                .with(user("john.doe"))
                                .param("base", "USD")
                                .param("quote", "PLN")
                                .accept(
                                        MediaType.APPLICATION_PROBLEM_JSON
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value(
                                "INVALID_REQUEST_PARAMETER"
                        ))
                .andExpect(jsonPath("$.detail")
                        .value(
                                "The required request parameter "
                                        + "'amount' is missing"
                        ));

        verifyNoInteractions(conversionService);
    }

    @Test
    void shouldReturnInvalidParameterForNonNumericAmount()
            throws Exception {
        mockMvc.perform(
                        get("/api/conversions")
                                .with(user("john.doe"))
                                .param("base", "USD")
                                .param("quote", "PLN")
                                .param(
                                        "amount",
                                        "not-a-number"
                                )
                                .accept(
                                        MediaType.APPLICATION_PROBLEM_JSON
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value(
                                "INVALID_REQUEST_PARAMETER"
                        ));

        verifyNoInteractions(conversionService);
    }
}
