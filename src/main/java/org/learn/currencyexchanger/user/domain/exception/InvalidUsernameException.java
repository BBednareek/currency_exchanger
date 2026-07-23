package org.learn.currencyexchanger.user.domain.exception;

public final class InvalidUsernameException extends RuntimeException {
    public InvalidUsernameException(String message) {
        super(message);
    }
}
