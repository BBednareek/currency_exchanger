package org.learn.currencyexchanger.user.api;

import jakarta.validation.Valid;
import org.learn.currencyexchanger.security.AppUserPrincipal;
import org.learn.currencyexchanger.user.application.UserService;
import org.learn.currencyexchanger.user.application.UserSnapshot;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserResponse getCurrentUser(@AuthenticationPrincipal AppUserPrincipal principal) {
        UserSnapshot user = userService.getUser(principal.getUserId());

        return UserApiMapper.toResponse(user);
    }

    @PatchMapping("me/username")
    public UserResponse changeUsername(@AuthenticationPrincipal AppUserPrincipal principal,
                                       @Valid @RequestBody ChangeUsernameRequest request) {
        UserSnapshot user = userService.changeUsername(principal.getUserId(), request.username());

        return UserApiMapper.toResponse(user);
    }

    public ResponseEntity<Void> disableAccount(@AuthenticationPrincipal AppUserPrincipal principal) {
        userService.disableOwnAccount(principal.getUserId());

        return ResponseEntity.noContent().build();
    }
}
