package org.learn.currencyexchanger.security.auth;

import org.jspecify.annotations.Nullable;
import org.learn.currencyexchanger.user.domain.User;
import org.learn.currencyexchanger.user.domain.UserStatus;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

// Kod zwiazany z logowaniem zawarty w security
// UserDatailsService jest kontraktem spring security uzywanym do pobierania danych uzytkownika na potrzeby uwierzytelnienia

public final class AppUserPrincipal implements
        UserDetails, CredentialsContainer {
    private final UUID userId;
    private final String username;
    private final Collection<? extends GrantedAuthority> authorities;
    private final UserStatus status;
    private @Nullable String passwordHash;

    private AppUserPrincipal(UUID userId, String username, String passwordHash, Collection<? extends GrantedAuthority> authorities, UserStatus status) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.authorities = List.copyOf(authorities);
        this.status = status;
    }

    public static AppUserPrincipal from(User user) {
        return new AppUserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getUserRole().name())),
                user.getStatus()
        );
    }

    @Override
    public void eraseCredentials() {
        passwordHash = null;
    }

    public UUID getUserId() {
        return userId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public @Nullable String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != UserStatus.LOCKED;
    }

    @Override
    public boolean isEnabled() {
        return status != UserStatus.DISABLED;
    }
}
