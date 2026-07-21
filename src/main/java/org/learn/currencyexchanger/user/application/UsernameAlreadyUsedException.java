package org.learn.currencyexchanger.user.application;

public class UsernameAlreadyUsedException extends RuntimeException {
    public UsernameAlreadyUsedException() {
        super("Username is already used");
    }
}
