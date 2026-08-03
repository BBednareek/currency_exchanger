package org.learn.currencyexchanger.rate.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.learn.currencyexchanger.TestcontainersConfiguration;
import org.learn.currencyexchanger.rate.application.port.ReferenceRateRepository;
import org.learn.currencyexchanger.rate.domain.CurrencyPair;
import org.learn.currencyexchanger.rate.domain.ReferenceRate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@Import({
        TestcontainersConfiguration.class,
        JpaReferenceRateRepositoryAdapter.class
})
class JpaReferenceRateRepositoryAdapterTest {

    private static final CurrencyPair PAIR =
            CurrencyPair.of("USD", "PLN");

    @Autowired
    private ReferenceRateRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static ReferenceRate rate(
            String value,
            String effectiveDate,
            String fetchedAt
    ) {
        return new ReferenceRate(
                PAIR,
                new BigDecimal(value),
                LocalDate.parse(effectiveDate),
                Instant.parse(fetchedAt)
        );
    }

    private static void assertNumericallyEqual(
            BigDecimal expected,
            BigDecimal actual
    ) {
        assertEquals(
                0,
                expected.compareTo(actual),
                () -> "Expected <%s> to be numerically equal to <%s>"
                        .formatted(expected, actual)
        );
    }

    @Test
    void shouldNotOverwriteRateWithEqualFetchTimestamp() {
        Instant fetchedAt =
                Instant.parse(
                        "2026-07-28T11:00:00Z"
                );

        repository.store(
                new ReferenceRate(
                        PAIR,
                        new BigDecimal("3.680000"),
                        LocalDate.of(2026, 7, 27),
                        fetchedAt
                )
        );

        ReferenceRate stored =
                repository.store(
                        new ReferenceRate(
                                PAIR,
                                new BigDecimal("3.670000"),
                                LocalDate.of(2026, 7, 27),
                                fetchedAt
                        )
                );

        assertNumericallyEqual(
                new BigDecimal("3.680000"),
                stored.value()
        );

        assertEquals(
                fetchedAt,
                stored.fetchedAt()
        );

        assertEquals(
                1L,
                countStoredRates()
        );
    }

    @Test
    void shouldStoreAndFindLatestReferenceRate() {
        ReferenceRate expected = rate(
                "3.672100",
                "2026-07-27",
                "2026-07-28T10:15:30Z"
        );

        repository.store(expected);

        ReferenceRate result =
                repository.findLatest(PAIR)
                        .orElseThrow();

        assertEquals(expected.pair(), result.pair());
        assertNumericallyEqual(
                expected.value(),
                result.value()
        );
        assertEquals(
                expected.effectiveDate(),
                result.effectiveDate()
        );
        assertEquals(
                expected.fetchedAt(),
                result.fetchedAt()
        );
    }

    @Test
    void shouldUpdateRateForTheSameEffectiveDate() {
        repository.store(rate(
                "3.670000",
                "2026-07-27",
                "2026-07-28T10:00:00Z"
        ));

        repository.store(rate(
                "3.680000",
                "2026-07-27",
                "2026-07-28T11:00:00Z"
        ));

        ReferenceRate result =
                repository.findLatest(PAIR)
                        .orElseThrow();

        assertNumericallyEqual(
                new BigDecimal("3.680000"),
                result.value()
        );

        assertEquals(
                Instant.parse("2026-07-28T11:00:00Z"),
                result.fetchedAt()
        );

        assertEquals(1L, countStoredRates());
    }

    @Test
    void shouldNotOverwriteNewerRateWithOlderObservation() {
        repository.store(rate(
                "3.680000",
                "2026-07-27",
                "2026-07-28T11:00:00Z"
        ));

        ReferenceRate stored = repository.store(rate(
                "3.670000",
                "2026-07-27",
                "2026-07-28T10:00:00Z"
        ));

        assertNumericallyEqual(
                new BigDecimal("3.680000"),
                stored.value()
        );

        assertEquals(
                Instant.parse("2026-07-28T11:00:00Z"),
                stored.fetchedAt()
        );

        assertEquals(1L, countStoredRates());
    }

    @Test
    void shouldKeepHistoryAndReturnLatestObservation() {
        repository.store(rate(
                "3.650000",
                "2026-07-25",
                "2026-07-25T10:00:00Z"
        ));

        ReferenceRate latest = rate(
                "3.680000",
                "2026-07-28",
                "2026-07-28T10:00:00Z"
        );

        repository.store(latest);

        ReferenceRate result =
                repository.findLatest(PAIR)
                        .orElseThrow();

        assertEquals(
                latest.effectiveDate(),
                result.effectiveDate()
        );

        assertNumericallyEqual(
                latest.value(),
                result.value()
        );

        assertEquals(2L, countStoredRates());
    }

    @Test
    void shouldEnforcePositiveRateAtDatabaseLevel() {
        assertThrows(
                DataIntegrityViolationException.class,
                () -> jdbcTemplate.update(
                        """
                                INSERT INTO reference_rate (
                                    id,
                                    base_currency,
                                    quote_currency,
                                    rate,
                                    effective_date,
                                    fetched_at
                                )
                                VALUES (?, ?, ?, ?, ?, ?)
                                """,
                        UUID.randomUUID(),
                        "USD",
                        "PLN",
                        BigDecimal.ZERO,
                        LocalDate.of(2026, 7, 28),
                        OffsetDateTime.parse(
                                "2026-07-28T10:00:00Z"
                        )
                )
        );
    }

    private long countStoredRates() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reference_rate",
                Long.class
        );

        return count == null ? 0L : count;
    }
}
