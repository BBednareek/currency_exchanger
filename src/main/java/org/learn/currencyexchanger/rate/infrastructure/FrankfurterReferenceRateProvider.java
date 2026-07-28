package org.learn.currencyexchanger.rate.infrastructure;

import org.learn.currencyexchanger.rate.application.exception.InvalidRateProviderResponseException;
import org.learn.currencyexchanger.rate.application.exception.RateProviderUnavailableException;
import org.learn.currencyexchanger.rate.application.exception.ReferenceRateNotFoundException;
import org.learn.currencyexchanger.rate.application.exception.UnsupportedCurrencyException;
import org.learn.currencyexchanger.rate.application.port.ReferenceRateProvider;
import org.learn.currencyexchanger.rate.domain.CurrencyPair;
import org.learn.currencyexchanger.rate.domain.ReferenceRate;
import org.learn.currencyexchanger.rate.domain.exception.InvalidCurrencyCodeException;
import org.learn.currencyexchanger.rate.domain.exception.InvalidCurrencyPairException;
import org.learn.currencyexchanger.rate.domain.exception.InvalidReferenceRateException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Clock;
import java.util.Objects;

public final class FrankfurterReferenceRateProvider
        implements ReferenceRateProvider {

    private final RestClient restClient;
    private final Clock clock;

    public FrankfurterReferenceRateProvider(
            @Qualifier("frankfurterRestClient")
            RestClient restClient,
            Clock clock
    ) {
        this.restClient = Objects.requireNonNull(
                restClient,
                "Rest Client cannot be null"
        );

        this.clock = Objects.requireNonNull(
                clock,
                "Clock cannot be null"
        );
    }


    @Override
    public ReferenceRate fetchLatest(CurrencyPair pair) {
        Objects.requireNonNull(
                pair,
                "Currency pair cannot be null"
        );

        FrankfurterRateResponse response;

        try {
            response = restClient
                    .get()
                    .uri(
                            "/rate/{base}/{quote}",
                            pair.base().value(),
                            pair.quote().value()
                    )
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .onStatus(
                            status -> status.value() == 400
                                    || status.value() == 422,
                            ((request, providerResponse) -> {
                                throw new UnsupportedCurrencyException(pair);
                            }
                            ))
                    .onStatus(
                            status -> status.value() == 404,
                            ((request, providerResponse) -> {
                                throw new ReferenceRateNotFoundException(pair);
                            })
                    )
                    .onStatus(
                            HttpStatusCode::isError,
                            (request, providerResponse) -> {
                                throw new RateProviderUnavailableException();
                            }
                    )
                    .body(FrankfurterRateResponse.class);
        } catch (ResourceAccessException exception) {
            throw new RateProviderUnavailableException(exception);
        } catch (RestClientException exception) {
            throw new InvalidRateProviderResponseException(exception);
        }
        return mapResponse(pair, response);
    }

    private ReferenceRate mapResponse(
            CurrencyPair requestedPair,
            FrankfurterRateResponse response
    ) {
        if (response == null) {
            throw new InvalidRateProviderResponseException(
                    "response body is missing"
            );
        }

        try {
            CurrencyPair responsePair = CurrencyPair.of(
                    response.base(),
                    response.quote()
            );

            if (!requestedPair.equals(responsePair)) {
                throw new InvalidRateProviderResponseException(
                        "returned currency pair does not match the request"
                );
            }

            return new ReferenceRate(
                    requestedPair,
                    response.rate(),
                    response.date(),
                    clock.instant()
            );
        } catch (
                InvalidCurrencyCodeException
                | InvalidCurrencyPairException
                | InvalidReferenceRateException
                | NullPointerException exception
        ) {
            throw new InvalidRateProviderResponseException(exception);
        }
    }
}
