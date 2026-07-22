package org.learn.currencyexchanger.user.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.learn.currencyexchanger.security.AppUserPrincipal;
import org.learn.currencyexchanger.user.application.UserService;
import org.learn.currencyexchanger.user.application.UserSnapshot;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.*;

// Pozwala uzytkowniki pobrac wlasne dane
// Odczytuje uzytkownika z kontekstu bezpieczenstwa
// Waliduje request
// Uruchamia przypadek uzycia
// Mapuje wynik na odpowiedz http
// nie wykonuje zapytan do repyztorum ani logiki biznesowej

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    private final SecurityContextLogoutHandler logoutHandler;

    public UserController(UserService userService, SecurityContextLogoutHandler logoutHandler) {
        this.userService = userService;
        this.logoutHandler = logoutHandler;
    }

    @GetMapping("/me")
    public UserResponse getCurrentUser(@AuthenticationPrincipal AppUserPrincipal principal) {
        UserSnapshot user = userService.getUser(principal.getUserId());

        return UserApiMapper.toResponse(user);
    }

    @PatchMapping("/me/username")
    public UserResponse changeUsername(@AuthenticationPrincipal AppUserPrincipal principal,
                                       @Valid @RequestBody ChangeUsernameRequest request) {
        UserSnapshot user = userService.changeUsername(principal.getUserId(), request.username());

        return UserApiMapper.toResponse(user);
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> disableAccount(
            @AuthenticationPrincipal AppUserPrincipal principal,
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        userService.disableOwnAccount(principal.getUserId());

        // Mechanizm wylogowania oparty na sesjach.
        logoutHandler.logout(request, response, authentication);

        return ResponseEntity.noContent().build();
    }
}
