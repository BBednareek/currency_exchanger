package org.learn.currencyexchanger.user.domain;

import org.junit.jupiter.api.Test;
import org.learn.currencyexchanger.user.domain.exception.DisabledUserCannotBeModifiedException;
import org.learn.currencyexchanger.user.domain.exception.UserCannotBeUnlockedException;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserTest {

    private static final String PASSWORD_HASH = "{bcrypt}password-hash";

    @Test
    void shouldRegisterActiveUserWithNormalizedUsername() {
        User user = User.register(
                "   John.DOE    ",
                PASSWORD_HASH
        );

        assertEquals("john.doe", user.getUsername());
        assertEquals(UserRole.USER, user.getUserRole());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertEquals(PASSWORD_HASH, user.getPasswordHash());
    }

    @Test
    void shouldNotModifyDisabledUser() {
        User user = User.register(
                "john.doe",
                PASSWORD_HASH
        );

        user.disable(Instant.parse("2026-07-27T10:15:30Z"));

        assertThrows(DisabledUserCannotBeModifiedException.class, () -> user.changeUsername("new.name"));
    }

    @Test
    void shouldRecordOnlyFirstAccountDisableTimestamp() {
        User user = User.register(
                "john.doe",
                PASSWORD_HASH
        );
        Instant firstDisableTime =
                Instant.parse("2026-07-27T10:15:30Z");

        assertNull(user.getDisabledAt());

        user.disable(firstDisableTime);
        user.disable(Instant.parse("2026-07-27T11:15:30Z"));

        assertEquals(UserStatus.DISABLED, user.getStatus());
        assertEquals(firstDisableTime, user.getDisabledAt());
    }

    @Test
    void shouldUnlockLockedUser() {
        User user = User.register(
                "john.doe",
                PASSWORD_HASH
        );

        user.lock();
        user.unlock();

        assertEquals(UserStatus.ACTIVE, user.getStatus());
    }

    @Test
    void shouldNotUnlockActiveUser() {
        User user = User.register(
                "john.doe",
                PASSWORD_HASH
        );

        assertThrows(UserCannotBeUnlockedException.class, user::unlock);
    }

    @Test
    void shouldRejectBlankPasswordHash() {
        assertThrows(IllegalArgumentException.class, () -> User.register("john.doe", " "));
    }

    @Test
    void shouldRejectTooLongPasswordHash() {
        String tooLongPasswordHash = "x".repeat(256);

        assertThrows(
                IllegalArgumentException.class,
                () -> User.register("john.doe", tooLongPasswordHash)
        );
    }

}
