package org.learn.currencyexchanger.rate.api;

import org.learn.currencyexchanger.rate.application.ReferenceRateService;
import org.learn.currencyexchanger.rate.application.ReferenceRateSnapshot;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rates")
public class ReferenceRateController {
    private final ReferenceRateService referenceRateService;

    public ReferenceRateController(
            ReferenceRateService referenceRateService
    ) {
        this.referenceRateService = referenceRateService;
    }

    @GetMapping("/{base}/{quote}")
    public ReferenceRateResponse getLatestRoute(
            @PathVariable String base,
            @PathVariable String quote
    ) {
        ReferenceRateSnapshot snapshot =
                referenceRateService.getLatestRate(base, quote);

        return ReferenceRateApiMapper.toResponse(snapshot);
    }
}
