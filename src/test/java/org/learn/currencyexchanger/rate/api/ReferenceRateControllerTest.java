package org.learn.currencyexchanger.rate.api;

import org.junit.jupiter.api.Test;
import org.learn.currencyexchanger.common.api.problem.ApiExceptionHandler;
import org.learn.currencyexchanger.common.api.problem.ApiProblemFactory;
import org.learn.currencyexchanger.rate.application.ReferenceRateService;
import org.learn.currencyexchanger.rate.application.ReferenceRateSnapshot;
import org.learn.currencyexchanger.rate.application.exception.RateProviderUnavailableException;
import org.learn.currencyexchanger.rate.domain.CurrencyCode;
import org.learn.currencyexchanger.rate.domain.exception.InvalidCurrencyCodeException;
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
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReferenceRateController.class)
@Import({
        ApiExceptionHandler.class,
        ApiProblemFactory.class
})
class ReferenceRateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReferenceRateService referenceRateService;

    @Test
    void shouldReturnLatestReferenceRate() throws Exception {
        ReferenceRateSnapshot snapshot =
                new ReferenceRateSnapshot(
                        new CurrencyCode("USD"),
                        new CurrencyCode("PLN"),
                        new BigDecimal("3.672100"),
                        LocalDate.of(2026, 7, 27),
                        Instant.parse("2026-07-28T10:15:30Z"),
                        false
                );

        when(referenceRateService.getLatestRate(
                "USD",
                "PLN"
        )).thenReturn(snapshot);

        mockMvc.perform(
                        get("/api/rates/USD/PLN")
                                .with(user("john.doe"))
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.base").value("USD"))
                .andExpect(jsonPath("$.quote").value("PLN"))
                .andExpect(jsonPath("$.rate").value(3.672100))
                .andExpect(jsonPath("$.effectiveDate")
                        .value("2026-07-27"))
                .andExpect(jsonPath("$.fetchedAt")
                        .value("2026-07-28T10:15:30Z"))
                .andExpect(jsonPath("$.stale").value(false)
                );

        verify(referenceRateService).getLatestRate(
                "USD",
                "PLN"
        );
    }

    @Test
    void shouldMarkFallbackRateAsStale() throws Exception {
        ReferenceRateSnapshot snapshot =
                new ReferenceRateSnapshot(
                        new CurrencyCode("USD"),
                        new CurrencyCode("PLN"),
                        new BigDecimal("3.640000"),
                        LocalDate.of(2026, 7, 27),
                        Instant.parse(
                                "2026-07-28T10:00:00Z"
                        ),
                        true
                );

        when(referenceRateService.getLatestRate(
                "USD",
                "PLN"
        )).thenReturn(snapshot);

        mockMvc.perform(
                        get("/api/rates/USD/PLN")
                                .with(user("john.doe"))
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.base")
                        .value("USD"))
                .andExpect(jsonPath("$.quote")
                        .value("PLN"))
                .andExpect(jsonPath("$.stale")
                        .value(true))
                .andExpect(jsonPath("$.fetchedAt")
                        .value("2026-07-28T10:00:00Z"));

        verify(referenceRateService).getLatestRate(
                "USD",
                "PLN"
        );
    }

    @Test
    void shouldReturnBadRequestForInvalidCurrencyCode()
            throws Exception {
        when(referenceRateService.getLatestRate(
                "US",
                "PLN"
        )).thenThrow(new InvalidCurrencyCodeException());

        mockMvc.perform(
                        get("/api/rates/US/PLN")
                                .with(user("john.doe"))
                                .accept(
                                        MediaType.APPLICATION_PROBLEM_JSON
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(jsonPath("$.code")
                        .value("INVALID_CURRENCY_CODE"))
                .andExpect(jsonPath("$.instance")
                        .value("/api/rates/US/PLN"));

        verify(referenceRateService).getLatestRate(
                "US",
                "PLN"
        );
    }

    @Test
    void shouldReturnServiceUnavailableWhenProviderFails()
            throws Exception {
        when(referenceRateService.getLatestRate(
                "USD",
                "PLN"
        )).thenThrow(
                new RateProviderUnavailableException()
        );

        mockMvc.perform(
                        get("/api/rates/USD/PLN")
                                .with(user("john.doe"))
                                .accept(
                                        MediaType.APPLICATION_PROBLEM_JSON
                                )
                )
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(jsonPath("$.code")
                        .value("RATE_PROVIDER_UNAVAILABLE"))
                .andExpect(jsonPath("$.detail").value(
                        "The reference rate provider is temporarily unavailable"
                ));

        verify(referenceRateService).getLatestRate(
                "USD",
                "PLN"
        );
    }
}
