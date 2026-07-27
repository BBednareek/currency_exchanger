package org.learn.currencyexchanger.auth.api;


import jakarta.validation.Valid;
import org.learn.currencyexchanger.auth.application.RegistrationResult;
import org.learn.currencyexchanger.auth.application.RegistrationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class RegistrationController {
    private final RegistrationService authService;

    public RegistrationController(RegistrationService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        RegistrationResult result = authService.register(
                request.username(),
                request.password()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(AuthApiMapper.toResponse(result));
    }
}
