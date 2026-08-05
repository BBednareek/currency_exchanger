package org.learn.currencyexchanger.auth.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.learn.currencyexchanger.auth.application.AuthenticationAttemptService;
import org.learn.currencyexchanger.auth.application.AuthenticationAttemptTicket;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class LoginController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final AuthenticationAttemptService authenticationAttemptService;
    private final SecurityContextHolderStrategy securityContextHolderStrategy;

    public LoginController(
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository,
            SessionAuthenticationStrategy sessionAuthenticationStrategy,
            AuthenticationAttemptService authenticationAttemptService
    ) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
        this.authenticationAttemptService = authenticationAttemptService;
        this.securityContextHolderStrategy =
                SecurityContextHolder.getContextHolderStrategy();
    }

    @PostMapping("/login")
    public ResponseEntity<Void> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        AuthenticationAttemptTicket attemptTicket =
                authenticationAttemptService.beginAttempt(
                        request.username()
                );

        Authentication authenticationRequest =
                UsernamePasswordAuthenticationToken.unauthenticated(
                        request.username(),
                        request.password()
                );

        Authentication authenticationResult;

        try {
            authenticationResult =
                    authenticationManager.authenticate(
                            authenticationRequest
                    );
        } catch (BadCredentialsException exception) {
            authenticationAttemptService.recordFailure(attemptTicket);
            throw exception;
        }

        authenticationAttemptService.recordSuccess(attemptTicket);

        sessionAuthenticationStrategy.onAuthentication(
                authenticationResult,
                httpRequest,
                httpResponse
        );

        SecurityContext securityContext =
                securityContextHolderStrategy.createEmptyContext();

        securityContext.setAuthentication(authenticationResult);
        securityContextHolderStrategy.setContext(securityContext);

        securityContextRepository.saveContext(
                securityContext,
                httpRequest,
                httpResponse
        );

        return ResponseEntity.noContent().build();
    }
}
