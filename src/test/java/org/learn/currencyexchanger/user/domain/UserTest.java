package org.learn.currencyexchanger.user.domain;

import org.junit.jupiter.api.Test;
import org.learn.currencyexchanger.user.domain.exception.DisabledUserCannotBeModifiedException;
import org.learn.currencyexchanger.user.domain.exception.UserCannotBeUnlockedException;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void shouldRegisterActiveUserWithNormalizedUsername() {
        User user = User.register(
                "   John.DOE    ",
                "{bcrypt}password-hash"
        );

        assertEquals("john.doe", user.getUsername());
        assertEquals(UserRole.USER, user.getUserRole());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertEquals("{bcrypt}password-hash", user.getPasswordHash());
    }

    @Test
    void shouldNotModifyDisabledUser() {
        User user = User.register(
                "john.doe",
                "{bcrypt}password-hash"
        );

        user.disable();

        assertThrows(DisabledUserCannotBeModifiedException.class, () -> user.changeUsername("new.name"));
    }

    @Test
    void shouldUnlockLockedUser() {
        User user = User.register(
                "john.doe",
                "{bcrypt}password-hash"
        );

        user.lock();
        user.unlock();

        assertEquals(UserStatus.ACTIVE, user.getStatus());
    }

    @Test
    void shouldNotUnlockActiveUser() {
        User user = User.register(
                "john.doe",
                "{bcrypt}password-hash"
        );

        assertThrows(UserCannotBeUnlockedException.class, user::unlock);
    }

    @Test
    void shouldRejectBlankPasswordHash() {
        assertThrows(IllegalArgumentException.class, () -> User.register("john.doe", " "));
    }

}