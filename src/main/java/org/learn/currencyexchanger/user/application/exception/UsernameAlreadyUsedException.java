package org.learn.currencyexchanger.user.application.exception;

public final class UsernameAlreadyUsedException
        extends RuntimeException {

    private static final String MESSAGE =
            "Username is already used";

    public UsernameAlreadyUsedException() {
        super(MESSAGE);
    }

    public UsernameAlreadyUsedException(Throwable cause) {
        super(MESSAGE, cause);
    }
}
