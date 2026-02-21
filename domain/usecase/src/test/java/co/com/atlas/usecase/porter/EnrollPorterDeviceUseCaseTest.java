package co.com.atlas.usecase.porter;

import co.com.atlas.model.auth.AuthUser;
import co.com.atlas.model.auth.UserStatus;
import co.com.atlas.model.auth.gateways.AuthUserRepository;
import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.common.NotFoundException;
import co.com.atlas.model.crypto.OrganizationCryptoKey;
import co.com.atlas.model.crypto.gateways.CryptoKeyGeneratorGateway;
import co.com.atlas.model.crypto.gateways.CryptoKeyRepository;
import co.com.atlas.model.organization.Organization;
import co.com.atlas.model.organization.gateways.OrganizationRepository;
import co.com.atlas.model.porter.DeviceInfo;
import co.com.atlas.model.porter.PorterEnrollmentAuditLog;
import co.com.atlas.model.porter.PorterEnrollmentToken;
import co.com.atlas.model.porter.PorterEnrollmentTokenStatus;
import co.com.atlas.model.porter.gateways.PorterEnrollmentAuditRepository;
import co.com.atlas.model.porter.gateways.PorterEnrollmentTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollPorterDeviceUseCaseTest {

    @Mock private PorterEnrollmentTokenRepository tokenRepository;
    @Mock private PorterEnrollmentAuditRepository auditRepository;
    @Mock private AuthUserRepository authUserRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private CryptoKeyRepository cryptoKeyRepository;
    @Mock private CryptoKeyGeneratorGateway cryptoKeyGeneratorGateway;

    private EnrollPorterDeviceUseCase useCase;

    private static final Long USER_ID = 10L;
    private static final Long ORG_ID = 100L;
    private static final String RAW_TOKEN = "enroll-abc123-token";
    private static final String TOKEN_HASH = ValidateEnrollmentTokenUseCase.hashToken(RAW_TOKEN);

    @BeforeEach
    void setUp() {
        useCase = new EnrollPorterDeviceUseCase(
                tokenRepository, auditRepository, authUserRepository,
                organizationRepository, cryptoKeyRepository, cryptoKeyGeneratorGateway);
    }

    @Test
    @DisplayName("Should enroll device successfully with existing crypto key")
    void shouldEnrollWithExistingKey() {
        // Arrange
        PorterEnrollmentToken token = buildPendingToken();
        OrganizationCryptoKey cryptoKey = buildCryptoKey();
        AuthUser user = buildPreRegisteredUser();
        AuthUser activatedUser = user.toBuilder().status(UserStatus.ACTIVE).active(true).build();
        Organization org = Organization.builder().id(ORG_ID).name("Conjunto El Bosque").build();

        when(tokenRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Mono.just(token));
        when(tokenRepository.save(any(PorterEnrollmentToken.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(cryptoKeyRepository.findActiveByOrganizationId(ORG_ID)).thenReturn(Mono.just(cryptoKey));
        when(authUserRepository.findById(USER_ID)).thenReturn(Mono.just(user));
        when(authUserRepository.save(any(AuthUser.class))).thenReturn(Mono.just(activatedUser));
        when(organizationRepository.findById(ORG_ID)).thenReturn(Mono.just(org));
        when(auditRepository.save(any(PorterEnrollmentAuditLog.class)))
                .thenReturn(Mono.just(PorterEnrollmentAuditLog.builder().id(1L).build()));

        var command = new EnrollPorterDeviceUseCase.EnrollCommand(
                RAW_TOKEN,
                new DeviceInfo("Android", "Samsung SM-T510", "1.0.0", "Mozilla/5.0"),
                "192.168.1.100", "Mozilla/5.0"
        );

        // Act & Assert
        StepVerifier.create(useCase.execute(command))
                .assertNext(result -> {
                    assertThat(result.porterId()).isEqualTo(USER_ID);
                    assertThat(result.porterDisplayName()).isEqualTo("Portería Principal");
                    assertThat(result.organizationName()).isEqualTo("Conjunto El Bosque");
                    assertThat(result.verificationKeyJwk()).isEqualTo("{\"kty\":\"OKP\",\"crv\":\"Ed25519\",\"x\":\"test\"}");
                    assertThat(result.keyId()).isEqualTo("key-uuid-123");
                    assertThat(result.maxClockSkewMinutes()).isEqualTo(5);
                })
                .verifyComplete();

        // Verify token was consumed
        ArgumentCaptor<PorterEnrollmentToken> tokenCaptor = ArgumentCaptor.forClass(PorterEnrollmentToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getStatus()).isEqualTo(PorterEnrollmentTokenStatus.CONSUMED);
        assertThat(tokenCaptor.getValue().getConsumedAt()).isNotNull();

        // Verify crypto key was NOT generated (existing key reused)
        verify(cryptoKeyGeneratorGateway, never()).generateForOrganization(anyLong());
    }

    @Test
    @DisplayName("Should generate crypto key when org has none")
    void shouldGenerateCryptoKeyWhenNoneExists() {
        PorterEnrollmentToken token = buildPendingToken();
        OrganizationCryptoKey newKey = buildCryptoKey();
        AuthUser user = buildPreRegisteredUser();
        Organization org = Organization.builder().id(ORG_ID).name("Conjunto El Bosque").build();

        when(tokenRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Mono.just(token));
        when(tokenRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(cryptoKeyRepository.findActiveByOrganizationId(ORG_ID)).thenReturn(Mono.empty());
        when(cryptoKeyGeneratorGateway.generateForOrganization(ORG_ID)).thenReturn(Mono.just(newKey));
        when(cryptoKeyRepository.save(newKey)).thenReturn(Mono.just(newKey));
        when(authUserRepository.findById(USER_ID)).thenReturn(Mono.just(user));
        when(authUserRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(organizationRepository.findById(ORG_ID)).thenReturn(Mono.just(org));
        when(auditRepository.save(any())).thenReturn(Mono.just(PorterEnrollmentAuditLog.builder().id(1L).build()));

        var command = new EnrollPorterDeviceUseCase.EnrollCommand(
                RAW_TOKEN, new DeviceInfo("iOS", "iPad", "1.0.0", "Safari"), "10.0.0.1", "Safari");

        StepVerifier.create(useCase.execute(command))
                .assertNext(result -> {
                    assertThat(result.verificationKeyJwk()).isNotBlank();
                    assertThat(result.keyId()).isEqualTo("key-uuid-123");
                })
                .verifyComplete();

        verify(cryptoKeyGeneratorGateway).generateForOrganization(ORG_ID);
        verify(cryptoKeyRepository).save(newKey);
    }

    @Test
    @DisplayName("Should be idempotent for already-active user")
    void shouldBeIdempotentForActiveUser() {
        PorterEnrollmentToken token = buildPendingToken();
        OrganizationCryptoKey cryptoKey = buildCryptoKey();
        AuthUser activeUser = AuthUser.builder()
                .id(USER_ID).names("Portería Principal")
                .status(UserStatus.ACTIVE).active(true).build();
        Organization org = Organization.builder().id(ORG_ID).name("Conjunto El Bosque").build();

        when(tokenRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Mono.just(token));
        when(tokenRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(cryptoKeyRepository.findActiveByOrganizationId(ORG_ID)).thenReturn(Mono.just(cryptoKey));
        when(authUserRepository.findById(USER_ID)).thenReturn(Mono.just(activeUser));
        when(organizationRepository.findById(ORG_ID)).thenReturn(Mono.just(org));
        when(auditRepository.save(any())).thenReturn(Mono.just(PorterEnrollmentAuditLog.builder().id(1L).build()));

        var command = new EnrollPorterDeviceUseCase.EnrollCommand(
                RAW_TOKEN, null, "10.0.0.1", "Mozilla/5.0");

        StepVerifier.create(useCase.execute(command))
                .assertNext(result -> assertThat(result.porterId()).isEqualTo(USER_ID))
                .verifyComplete();

        // User save NOT called because user was already ACTIVE
        verify(authUserRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject blank token")
    void shouldRejectBlankToken() {
        var command = new EnrollPorterDeviceUseCase.EnrollCommand("", null, "10.0.0.1", "UA");

        StepVerifier.create(useCase.execute(command))
                .expectErrorMatches(e -> e instanceof BusinessException
                        && ((BusinessException) e).getErrorCode().equals("TOKEN_REQUIRED"))
                .verify();
    }

    @Test
    @DisplayName("Should reject consumed token")
    void shouldRejectConsumedToken() {
        PorterEnrollmentToken consumed = buildPendingToken().toBuilder()
                .status(PorterEnrollmentTokenStatus.CONSUMED).build();
        when(tokenRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Mono.just(consumed));

        var command = new EnrollPorterDeviceUseCase.EnrollCommand(RAW_TOKEN, null, "10.0.0.1", "UA");

        StepVerifier.create(useCase.execute(command))
                .expectErrorMatches(e -> e instanceof BusinessException
                        && ((BusinessException) e).getErrorCode().equals("TOKEN_ALREADY_USED"))
                .verify();
    }

    @Test
    @DisplayName("Should reject expired token")
    void shouldRejectExpiredToken() {
        PorterEnrollmentToken expired = PorterEnrollmentToken.builder()
                .id(1L).userId(USER_ID).organizationId(ORG_ID).tokenHash(TOKEN_HASH)
                .status(PorterEnrollmentTokenStatus.PENDING)
                .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .createdAt(Instant.now()).build();

        when(tokenRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Mono.just(expired));

        var command = new EnrollPorterDeviceUseCase.EnrollCommand(RAW_TOKEN, null, "10.0.0.1", "UA");

        StepVerifier.create(useCase.execute(command))
                .expectErrorMatches(e -> e instanceof BusinessException
                        && ((BusinessException) e).getErrorCode().equals("TOKEN_EXPIRED"))
                .verify();
    }

    @Test
    @DisplayName("Should reject unknown token")
    void shouldRejectUnknownToken() {
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Mono.empty());

        var command = new EnrollPorterDeviceUseCase.EnrollCommand("unknown-token", null, "10.0.0.1", "UA");

        StepVerifier.create(useCase.execute(command))
                .expectError(NotFoundException.class)
                .verify();
    }

    @Test
    @DisplayName("Should save audit log with device info")
    void shouldSaveAuditLogWithDeviceInfo() {
        PorterEnrollmentToken token = buildPendingToken();
        OrganizationCryptoKey cryptoKey = buildCryptoKey();
        AuthUser user = buildPreRegisteredUser();
        Organization org = Organization.builder().id(ORG_ID).name("Conjunto El Bosque").build();

        when(tokenRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Mono.just(token));
        when(tokenRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(cryptoKeyRepository.findActiveByOrganizationId(ORG_ID)).thenReturn(Mono.just(cryptoKey));
        when(authUserRepository.findById(USER_ID)).thenReturn(Mono.just(user));
        when(authUserRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(organizationRepository.findById(ORG_ID)).thenReturn(Mono.just(org));
        when(auditRepository.save(any())).thenReturn(Mono.just(PorterEnrollmentAuditLog.builder().id(1L).build()));

        var command = new EnrollPorterDeviceUseCase.EnrollCommand(
                RAW_TOKEN,
                new DeviceInfo("Android", "Pixel 7", "2.0.0", "Chrome"),
                "192.168.1.50", "Chrome/120"
        );

        StepVerifier.create(useCase.execute(command)).expectNextCount(1).verifyComplete();

        ArgumentCaptor<PorterEnrollmentAuditLog> auditCaptor =
                ArgumentCaptor.forClass(PorterEnrollmentAuditLog.class);
        verify(auditRepository).save(auditCaptor.capture());

        PorterEnrollmentAuditLog log = auditCaptor.getValue();
        assertThat(log.getIpAddress()).isEqualTo("192.168.1.50");
        assertThat(log.getUserAgent()).isEqualTo("Chrome/120");
        assertThat(log.getDetails()).contains("Android");
        assertThat(log.getDetails()).contains("Pixel 7");
    }

    // ---- Helpers ----

    private PorterEnrollmentToken buildPendingToken() {
        return PorterEnrollmentToken.builder()
                .id(1L)
                .userId(USER_ID)
                .organizationId(ORG_ID)
                .tokenHash(TOKEN_HASH)
                .status(PorterEnrollmentTokenStatus.PENDING)
                .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
                .createdAt(Instant.now())
                .build();
    }

    private OrganizationCryptoKey buildCryptoKey() {
        return OrganizationCryptoKey.builder()
                .id(1L)
                .organizationId(ORG_ID)
                .algorithm("Ed25519")
                .keyId("key-uuid-123")
                .publicKeyJwk("{\"kty\":\"OKP\",\"crv\":\"Ed25519\",\"x\":\"test\"}")
                .privateKeyEncrypted("encrypted-data")
                .isActive(true)
                .createdAt(Instant.now())
                .build();
    }

    private AuthUser buildPreRegisteredUser() {
        return AuthUser.builder()
                .id(USER_ID)
                .names("Portería Principal")
                .email("porter-xxx@test.atlas.internal")
                .status(UserStatus.PRE_REGISTERED)
                .active(false)
                .build();
    }
}
