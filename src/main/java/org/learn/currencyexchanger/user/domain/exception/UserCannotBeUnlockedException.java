package org.learn.currencyexchanger.user.domain.exception;

import org.learn.currencyexchanger.user.domain.UserStatus;

public final class UserCannotBeUnlockedException extends RuntimeException {
    public UserCannotBeUnlockedException(UserStatus status) {
        super(
                "User with status " + status + " cannot be unlocked"
        );
    }
}
