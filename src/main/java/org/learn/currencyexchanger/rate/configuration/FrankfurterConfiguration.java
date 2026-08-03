package org.learn.currencyexchanger.rate.configuration;

import org.learn.currencyexchanger.rate.application.port.ReferenceRateProvider;
import org.learn.currencyexchanger.rate.infrastructure.FrankfurterReferenceRateProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Clock;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(FrankfurterProperties.class)
public class FrankfurterConfiguration {

    @Bean
    public ReferenceRateProvider referenceRateProvider(
            @Qualifier("frankfurterRestClient")
            RestClient restClient,
            Clock clock,
            FrankfurterProperties properties
    ) {
        return new FrankfurterReferenceRateProvider(
                restClient,
                clock,
                properties.maximumEffectiveDateAgeDays()
        );
    }

    @Bean
    public RestClient frankfurterRestClient(
            RestClient.Builder restClientBuilder,
            FrankfurterProperties properties
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(
                        properties.connectTimeout()
                )
                .followRedirects(
                        HttpClient.Redirect.NEVER
                )
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(
                properties.readTimeout()
        );

        return restClientBuilder
                .baseUrl(properties.baseUrl())
                .defaultHeader(
                        HttpHeaders.ACCEPT,
                        MediaType.APPLICATION_JSON_VALUE
                )
                .requestFactory(requestFactory)
                .build();
    }
}
