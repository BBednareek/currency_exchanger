package org.learn.currencyexchanger.user.domain.exception;

public final class DisabledUserCannotBeModifiedException extends RuntimeException {
    public DisabledUserCannotBeModifiedException() {
        super(
                "Disabled user cannot be modified"
        );
    }
}
