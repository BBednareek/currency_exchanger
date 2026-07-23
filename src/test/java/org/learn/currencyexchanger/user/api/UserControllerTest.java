package org.learn.currencyexchanger.user.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.learn.currencyexchanger.common.api.ApiExceptionHandler;
import org.learn.currencyexchanger.security.AppUserPrincipal;
import org.learn.currencyexchanger.user.application.UserService;
import org.learn.currencyexchanger.user.application.UserSnapshot;
import org.learn.currencyexchanger.user.application.exception.UserNotFoundException;
import org.learn.currencyexchanger.user.application.exception.UsernameAlreadyUsedException;
import org.learn.currencyexchanger.user.domain.User;
import org.learn.currencyexchanger.user.domain.UserRole;
import org.learn.currencyexchanger.user.domain.UserStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(ApiExceptionHandler.class)
class UserControllerTest {
    private static final String PASSWORD_HASH = "{bcrypt}password-hash";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private SecurityContextLogoutHandler logoutHandler;

    private AppUserPrincipal principal;
    private UUID userId;

    @BeforeEach
    void setUp() {
        User user = User.register("john.doe", PASSWORD_HASH);

        principal = AppUserPrincipal.from(user);
        userId = principal.getUserId();
    }

    @Test
    void shouldReturnCurrentUser() throws Exception {
        UserSnapshot snapshot = snapshot("john.doe");

        when(userService.getUser(userId)).thenReturn(snapshot);

        mockMvc.perform(
                        get("/api/users/me").
                                with(user(principal))
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.username").value("john.doe"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(userService).getUser(userId);
    }

    @Test
    void shouldReturnNotFoundWhenCurrentUserDoesNotExist() throws Exception {
        when(userService.getUser(userId)).thenThrow(new UserNotFoundException(userId));

        mockMvc.perform(
                        get("/api/users/me")
                                .with(user(principal))
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
                .andExpect(jsonPath("$.title").value("User not found"));

        verify(userService).getUser(userId);
    }

    @Test
    void shouldChangeUsername() throws Exception {
        UserSnapshot snapshot = snapshot("new.username");

        when(userService.changeUsername(
                userId, "New.Username"
        )).thenReturn(snapshot);

        mockMvc.perform(
                        patch("/api/users/me/username")
                                .with(user(principal))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                        "username": "New.Username"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.username").
                        value("new.username"));

        verify(userService).changeUsername(userId, "New.Username");
    }

    @Test
    void shouldReturnValidationProblemForInvalidUsername() throws Exception {
        mockMvc.perform(
                        patch("/api/users/me/username")
                                .with(user(principal))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                        "username": "ab"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.violations[0].path").value("username"))
                .andExpect(jsonPath("$.violations[0].message").exists());

        verifyNoInteractions(userService);
    }

    @Test
    void shouldReturnConflictWhenUsernameIsAlreadyUsed() throws Exception {
        when(userService.changeUsername(
                userId, "existing.user"
        )).thenThrow(new UsernameAlreadyUsedException());

        mockMvc.perform(
                        patch("/api/users/me/username")
                                .with(user(principal))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                        "username": "existing.user"
                                        }
                                        """)
                )
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("USERNAME_ALREADY_USED"))
                .andExpect(jsonPath("$.title").value("Username already used"));

        verify(userService).changeUsername(userId, "existing.user");
    }

    @Test
    void shouldDisableAccountAndLogoutCurrentSession() throws Exception {
        doNothing().when(userService).disableOwnAccount(userId);

        mockMvc.perform(
                        delete("/api/users/me")
                                .with(user(principal))
                                .with(csrf())

                )
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(userService).disableOwnAccount(userId);

        verify(logoutHandler).logout(
                any(),
                any(),
                any(Authentication.class)
        );
    }

    private UserSnapshot snapshot(String username) {
        return new UserSnapshot(
                userId,
                username,
                UserRole.USER,
                UserStatus.ACTIVE
        );
    }

}