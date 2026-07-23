package org.learn.currencyexchanger.auth.exception;

public final class InvalidPasswordException extends RuntimeException {
    public InvalidPasswordException(String message) {
        super(message);
    }
}
