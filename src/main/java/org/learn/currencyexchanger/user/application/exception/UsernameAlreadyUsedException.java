package org.learn.currencyexchanger.user.application.exception;

public class UsernameAlreadyUsedException extends RuntimeException {
    public UsernameAlreadyUsedException() {
        super("Username is already used");
    }
}
