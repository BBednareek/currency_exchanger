package org.learn.currencyexchanger.rate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.learn.currencyexchanger.TestcontainersConfiguration;
import org.learn.currencyexchanger.rate.application.exception.RateProviderUnavailableException;
import org.learn.currencyexchanger.rate.application.port.ReferenceRateProvider;
import org.learn.currencyexchanger.rate.application.port.ReferenceRateRepository;
import org.learn.currencyexchanger.rate.domain.CurrencyPair;
import org.learn.currencyexchanger.rate.domain.ReferenceRate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Import(TestcontainersConfiguration.class)
@Transactional
class ReferenceRateFlowIntegrationTest {

    private static final CurrencyPair PAIR =
            CurrencyPair.of("USD", "PLN");

    private static final Instant CURRENT_TIME =
            Instant.parse("2026-07-28T12:00:00Z");

    private static final ReferenceRate PROVIDER_RATE =
            new ReferenceRate(
                    PAIR,
                    new BigDecimal("3.672100"),
                    LocalDate.of(2026, 7, 28),
                    CURRENT_TIME
            );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReferenceRateRepository referenceRateRepository;

    @MockitoBean
    private ReferenceRateProvider referenceRateProvider;

    @MockitoBean
    private Clock clock;

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(
                CURRENT_TIME
        );
    }

    @Test
    void shouldFetchPersistAndReuseReferenceRate()
            throws Exception {
        when(referenceRateProvider.fetchLatest(PAIR))
                .thenReturn(PROVIDER_RATE);

        performGetRate()
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.base")
                        .value("USD"))
                .andExpect(jsonPath("$.quote")
                        .value("PLN"))
                .andExpect(jsonPath("$.rate")
                        .value(3.672100))
                .andExpect(jsonPath("$.effectiveDate")
                        .value("2026-07-28"))
                .andExpect(jsonPath("$.fetchedAt")
                        .value("2026-07-28T12:00:00Z"))
                .andExpect(jsonPath("$.stale")
                        .value(false));

        /*
         * Drugie żądanie powinno wykorzystać świeży rekord
         * z PostgreSQL i nie wywoływać ponownie dostawcy.
         */
        performGetRate()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rate")
                        .value(3.672100))
                .andExpect(jsonPath("$.stale")
                        .value(false));

        verify(
                referenceRateProvider,
                times(1)
        ).fetchLatest(PAIR);

        ReferenceRate storedRate =
                referenceRateRepository
                        .findLatest(PAIR)
                        .orElseThrow();

        assertEquals(
                0,
                PROVIDER_RATE.value()
                        .compareTo(storedRate.value())
        );

        assertEquals(
                PROVIDER_RATE.effectiveDate(),
                storedRate.effectiveDate()
        );

        assertEquals(
                PROVIDER_RATE.fetchedAt(),
                storedRate.fetchedAt()
        );
    }

    @Test
    void shouldReturnStaleStoredRateWhenProviderIsUnavailable()
            throws Exception {
        ReferenceRate staleRate = new ReferenceRate(
                PAIR,
                new BigDecimal("3.640000"),
                LocalDate.of(2026, 7, 27),
                CURRENT_TIME.minus(
                        Duration.ofHours(2)
                )
        );

        referenceRateRepository.store(staleRate);

        when(referenceRateProvider.fetchLatest(PAIR))
                .thenThrow(
                        new RateProviderUnavailableException()
                );

        performGetRate()
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.base")
                        .value("USD"))
                .andExpect(jsonPath("$.quote")
                        .value("PLN"))
                .andExpect(jsonPath("$.rate")
                        .value(3.640000))
                .andExpect(jsonPath("$.effectiveDate")
                        .value("2026-07-27"))
                .andExpect(jsonPath("$.fetchedAt")
                        .value("2026-07-28T10:00:00Z"))
                .andExpect(jsonPath("$.stale")
                        .value(true));

        verify(referenceRateProvider)
                .fetchLatest(PAIR);
    }

    @Test
    void shouldReturnServiceUnavailableWhenFallbackIsTooOld()
            throws Exception {
        ReferenceRate tooOldRate = new ReferenceRate(
                PAIR,
                new BigDecimal("3.600000"),
                LocalDate.of(2026, 7, 20),
                CURRENT_TIME.minus(
                        Duration.ofDays(8)
                )
        );

        referenceRateRepository.store(tooOldRate);

        when(referenceRateProvider.fetchLatest(PAIR))
                .thenThrow(
                        new RateProviderUnavailableException()
                );

        performGetRate()
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(jsonPath("$.code")
                        .value("RATE_PROVIDER_UNAVAILABLE"))
                .andExpect(jsonPath("$.title")
                        .value("Rate provider unavailable"))
                .andExpect(jsonPath("$.detail")
                        .value(
                                "The reference rate provider "
                                        + "is temporarily unavailable"
                        ))
                .andExpect(jsonPath("$.instance")
                        .value("/api/rates/USD/PLN"));

        verify(referenceRateProvider)
                .fetchLatest(PAIR);
    }

    private org.springframework.test.web.servlet.ResultActions
    performGetRate() throws Exception {
        return mockMvc.perform(
                get("/api/rates/USD/PLN")
                        .accept(MediaType.APPLICATION_JSON)
        );
    }
}
