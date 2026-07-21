package org.learn.currencyexchanger;

import org.springframework.boot.SpringApplication;

public class TestCurrencyExchangerApplication {

    public static void main(String[] args) {
        SpringApplication.from(CurrencyExchangerApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
