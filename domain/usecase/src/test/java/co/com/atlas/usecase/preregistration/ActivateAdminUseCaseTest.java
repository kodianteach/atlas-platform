package co.com.atlas.usecase.preregistration;

import co.com.atlas.model.auth.AuthUser;
import co.com.atlas.model.auth.UserStatus;
import co.com.atlas.model.auth.gateways.AuthUserRepository;
import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.notification.gateways.NotificationGateway;
import co.com.atlas.model.preregistration.AdminActivationToken;
import co.com.atlas.model.preregistration.ActivationTokenStatus;
import co.com.atlas.model.preregistration.gateways.AdminActivationTokenRepository;
import co.com.atlas.model.preregistration.gateways.PreRegistrationAuditRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivateAdminUseCaseTest {

    @Mock
    private AuthUserRepository authUserRepository;

    @Mock
    private AdminActivationTokenRepository tokenRepository;

    @Mock
    private PreRegistrationAuditRepository auditRepository;

    @Mock
    private NotificationGateway notificationGateway;

    private ActivateAdminUseCase useCase;

    private AdminActivationToken validToken;
    private AuthUser existingUser;

    @BeforeEach
    void setUp() {
        useCase = new ActivateAdminUseCase(
                authUserRepository,
                tokenRepository,
                auditRepository,
                notificationGateway
        );

        validToken = AdminActivationToken.builder()
                .id(1L)
                .userId(100L)
                .tokenHash("hashed-token")
                .status(ActivationTokenStatus.PENDING)
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        existingUser = AuthUser.builder()
                .id(100L)
                .email("admin@test.com")
                .names("Test Admin")
                .passwordHash("$2a$10$hashedTempPassword")
                .status(UserStatus.PENDING_ACTIVATION)
                .active(false)
                .build();
    }

    @Nested
    @DisplayName("Password Policy Validation")
    class PasswordPolicyTests {

        private ActivateAdminUseCase.ActivateCommand commandWithPassword(String newPassword) {
            return new ActivateAdminUseCase.ActivateCommand(
                    "raw-token",
                    "admin@test.com",
                    "TempPass1",
                    newPassword,
                    "127.0.0.1",
                    "Test-Agent"
            );
        }

        private void setupValidTokenAndCredentials() {
            when(tokenRepository.findByTokenHash(anyString()))
                    .thenReturn(Mono.just(validToken));
            when(authUserRepository.findById(anyLong()))
                    .thenReturn(Mono.just(existingUser));
            when(authUserRepository.validatePassword(anyString(), anyString()))
                    .thenReturn(Mono.just(true));
        }

        @Test
        @DisplayName("Should accept valid password meeting all criteria")
        void shouldAcceptValidPassword() {
            setupValidTokenAndCredentials();
            when(authUserRepository.save(any(AuthUser.class)))
                    .thenReturn(Mono.just(existingUser.toBuilder()
                            .status(UserStatus.ACTIVATED)
                            .active(true)
                            .build()));
            when(tokenRepository.save(any(AdminActivationToken.class)))
                    .thenReturn(Mono.just(validToken));
            when(auditRepository.save(any()))
                    .thenReturn(Mono.empty());
            when(notificationGateway.sendActivationConfirmationEmail(anyString(), anyString()))
                    .thenReturn(Mono.empty());

            StepVerifier.create(useCase.execute(commandWithPassword("ValidPass1")))
                    .expectNextMatches(result -> result.email().equals("admin@test.com"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should reject password shorter than 8 characters")
        void shouldRejectShortPassword() {
            setupValidTokenAndCredentials();

            StepVerifier.create(useCase.execute(commandWithPassword("Ab1cde")))
                    .expectErrorMatches(ex -> ex instanceof BusinessException
                            && ((BusinessException) ex).getErrorCode().equals("WEAK_PASSWORD")
                            && ex.getMessage().contains("al menos 8 caracteres"))
                    .verify();
        }

        @Test
        @DisplayName("Should reject password without uppercase letter")
        void shouldRejectPasswordWithoutUppercase() {
            setupValidTokenAndCredentials();

            StepVerifier.create(useCase.execute(commandWithPassword("lowercase1")))
                    .expectErrorMatches(ex -> ex instanceof BusinessException
                            && ((BusinessException) ex).getErrorCode().equals("WEAK_PASSWORD")
                            && ex.getMessage().contains("mayúscula"))
                    .verify();
        }

        @Test
        @DisplayName("Should reject password without lowercase letter")
        void shouldRejectPasswordWithoutLowercase() {
            setupValidTokenAndCredentials();

            StepVerifier.create(useCase.execute(commandWithPassword("UPPERCASE1")))
                    .expectErrorMatches(ex -> ex instanceof BusinessException
                            && ((BusinessException) ex).getErrorCode().equals("WEAK_PASSWORD")
                            && ex.getMessage().contains("minúscula"))
                    .verify();
        }

        @Test
        @DisplayName("Should reject password without digit")
        void shouldRejectPasswordWithoutDigit() {
            setupValidTokenAndCredentials();

            StepVerifier.create(useCase.execute(commandWithPassword("NoDigitsHere")))
                    .expectErrorMatches(ex -> ex instanceof BusinessException
                            && ((BusinessException) ex).getErrorCode().equals("WEAK_PASSWORD")
                            && ex.getMessage().contains("número"))
                    .verify();
        }

        @Test
        @DisplayName("Should accept password with allowed special characters")
        void shouldAcceptPasswordWithAllowedSpecialChars() {
            setupValidTokenAndCredentials();
            when(authUserRepository.save(any(AuthUser.class)))
                    .thenReturn(Mono.just(existingUser.toBuilder()
                            .status(UserStatus.ACTIVATED)
                            .active(true)
                            .build()));
            when(tokenRepository.save(any(AdminActivationToken.class)))
                    .thenReturn(Mono.just(validToken));
            when(auditRepository.save(any()))
                    .thenReturn(Mono.empty());
            when(notificationGateway.sendActivationConfirmationEmail(anyString(), anyString()))
                    .thenReturn(Mono.empty());

            StepVerifier.create(useCase.execute(commandWithPassword("Valid@Pass1")))
                    .expectNextMatches(result -> result.email().equals("admin@test.com"))
                    .verifyComplete();
        }
    }
}
