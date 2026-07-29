package org.learn.currencyexchanger.user.infrastructure.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.learn.currencyexchanger.user.application.exception.ConcurrentUserModificationException;
import org.learn.currencyexchanger.user.domain.User;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaUserRepositoryAdapterUnitTest {

    private static final String PASSWORD_HASH =
            "{bcrypt}password-hash";

    @Mock
    private SpringDataUserRepository springDataUserRepository;

    private JpaUserRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaUserRepositoryAdapter(
                springDataUserRepository
        );
    }

    @Test
    void shouldTranslateOptimisticLockingFailure() {
        User user = User.register(
                "concurrent.user",
                PASSWORD_HASH
        );

        OptimisticLockingFailureException persistenceException =
                new OptimisticLockingFailureException(
                        "Sensitive persistence details"
                );

        when(springDataUserRepository.saveAndFlush(user))
                .thenThrow(persistenceException);

        ConcurrentUserModificationException exception =
                assertThrows(
                        ConcurrentUserModificationException.class,
                        () -> adapter.save(user)
                );

        assertSame(
                persistenceException,
                exception.getCause()
        );

        verify(springDataUserRepository)
                .saveAndFlush(user);
    }
}
