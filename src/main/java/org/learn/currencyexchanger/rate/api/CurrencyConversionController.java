package org.learn.currencyexchanger.rate.api;

import jakarta.validation.constraints.Digits;
import org.learn.currencyexchanger.rate.application.ConversionSnapshot;
import org.learn.currencyexchanger.rate.application.CurrencyConversionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/conversions")
public class CurrencyConversionController {
    private final CurrencyConversionService conversionService;

    public CurrencyConversionController(
            CurrencyConversionService conversionService
    ) {
        this.conversionService = conversionService;
    }

    @GetMapping
    public ConversionResponse convert(
            @RequestParam(name = "base")
            String base,

            @RequestParam(name = "quote")
            String quote,

            @RequestParam(name = "amount")
            @Digits(
                    integer = 18,
                    fraction = 8,
                    message = "Amount can contain at most " +
                            "18 integer and 8 fractional digits"
            )
            BigDecimal amount
    ) {
        ConversionSnapshot snapshot =
                conversionService.convert(
                        base,
                        quote,
                        amount
                );

        return ConversionApiMapper.toResponse(
                snapshot
        );
    }
}
