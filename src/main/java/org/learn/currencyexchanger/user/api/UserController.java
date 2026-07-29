package org.learn.currencyexchanger.user.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.learn.currencyexchanger.user.application.UserService;
import org.learn.currencyexchanger.user.application.UserSnapshot;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

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
    private final LogoutHandler logoutHandler;

    public UserController(
            UserService userService,
            LogoutHandler logoutHandler
    ) {
        this.userService = userService;
        this.logoutHandler = logoutHandler;
    }

    @GetMapping("/me")
    public UserResponse getCurrentUser(
            @AuthenticationPrincipal(
                    expression = "userId",
                    errorOnInvalidType = true
            )
            UUID userId
    ) {
        UserSnapshot user = userService.getUser(userId);

        return UserApiMapper.toResponse(user);
    }

    @PatchMapping("/me/username")
    public UserResponse changeUsername(
            @AuthenticationPrincipal(
                    expression = "userId",
                    errorOnInvalidType = true
            )
            UUID userId,
            @Valid @RequestBody ChangeUsernameRequest request
    ) {
        UserSnapshot user = userService.changeUsername(
                userId,
                request.username()
        );

        return UserApiMapper.toResponse(user);
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> disableAccount(
            @AuthenticationPrincipal(
                    expression = "userId",
                    errorOnInvalidType = true
            )
            UUID userId,
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        userService.disableOwnAccount(userId);

        // Mechanizm wylogowania oparty na sesjach.
        logoutHandler.logout(request, response, authentication);

        return ResponseEntity.noContent().build();
    }
}
