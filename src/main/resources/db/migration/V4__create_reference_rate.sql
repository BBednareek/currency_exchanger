CREATE TABLE reference_rate
(
    id             UUID           NOT NULL,
    base_currency  VARCHAR(3)     NOT NULL,
    quote_currency VARCHAR(3)     NOT NULL,
    rate           NUMERIC(38,18) NOT NULL,
    effective_date DATE           NOT NULL,
    fetched_at     TIMESTAMPTZ    NOT NULL,

    CONSTRAINT pk_reference_rate
        PRIMARY KEY (id),

    CONSTRAINT uk_reference_rate_pair_effective_date
        UNIQUE (
                base_currency,
                quote_currency,
                effective_date
            ),

    CONSTRAINT ck_reference_rate_base_currency
        CHECK (base_currency ~ '^[A-Z]{3}$'),

    CONSTRAINT ck_reference_rate_quote_currency
        CHECK (quote_currency ~ '^[A-Z]{3}$'),

    CONSTRAINT ck_reference_rate_different_currencies
        CHECK (base_currency <> quote_currency),

    CONSTRAINT ck_reference_rate_positive
        CHECK (rate > 0)
);

CREATE INDEX ix_reference_rate_pair_fetched_at
    ON reference_rate (
                       base_currency,
                       quote_currency,
                       fetched_at DESC
        );
