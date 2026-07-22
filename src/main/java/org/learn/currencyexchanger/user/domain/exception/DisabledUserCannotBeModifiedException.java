package org.learn.currencyexchanger.user.domain.exception;

public class DisabledUserCannotBeModifiedException extends RuntimeException {
    public DisabledUserCannotBeModifiedException() {
        super(
                "Disabled user cannot be modified"
        );
    }
}
