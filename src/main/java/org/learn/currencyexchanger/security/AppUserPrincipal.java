package org.learn.currencyexchanger.security;
import org.jspecify.annotations.NullMarked;
import org.learn.currencyexchanger.user.domain.User;
import org.learn.currencyexchanger.user.domain.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

// Kod zwiazany z logowaniem zawarty w security
// UserDatailsService jest kontraktem spring security uzywanym do pobierania danych uzytkownika na potrzeby uwierzytelnienia

public final class AppUserPrincipal implements UserDetails {
    private final UUID userId;
    private final String username;
    private final String passwordHash;
    private final Collection<? extends GrantedAuthority> authorities;
    private final UserStatus status;

    private AppUserPrincipal(UUID userId, String username, String passwordHash, Collection<? extends GrantedAuthority> authorities, UserStatus status) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.authorities = authorities;
        this.status = status;
    }

    public static AppUserPrincipal from(User user) {
        return new AppUserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                List.of(new SimpleGrantedAuthority("ROLE_ " + user.getUserRole().name())),
                user.getStatus()
        );
    }

    public UUID getUserId() {
        return userId;
    }

    @NullMarked
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    public String getPassword() {
        return passwordHash;
    }

    @NullMarked
    @Override
    public String getUsername() {
        return username;
    }

    @NullMarked
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != UserStatus.LOCKED;
    }

    @NullMarked
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }
}