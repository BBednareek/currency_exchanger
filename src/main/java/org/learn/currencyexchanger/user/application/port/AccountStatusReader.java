package org.learn.currencyexchanger.user.application.port;

import java.util.UUID;

public interface AccountStatusReader {
    //Returns true only when the account exists and its ACTIVE
    boolean isActive(UUID userId);
}
