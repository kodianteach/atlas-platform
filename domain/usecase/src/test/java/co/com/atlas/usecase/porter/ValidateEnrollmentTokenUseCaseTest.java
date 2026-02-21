package co.com.atlas.usecase.porter;

import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.common.NotFoundException;
import co.com.atlas.model.organization.Organization;
import co.com.atlas.model.organization.gateways.OrganizationRepository;
import co.com.atlas.model.porter.Porter;
import co.com.atlas.model.porter.PorterEnrollmentToken;
import co.com.atlas.model.porter.PorterEnrollmentTokenStatus;
import co.com.atlas.model.porter.PorterType;
import co.com.atlas.model.porter.gateways.PorterEnrollmentTokenRepository;
import co.com.atlas.model.porter.gateways.PorterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidateEnrollmentTokenUseCaseTest {

    @Mock private PorterEnrollmentTokenRepository tokenRepository;
    @Mock private PorterRepository porterRepository;
    @Mock private OrganizationRepository organizationRepository;

    private ValidateEnrollmentTokenUseCase useCase;

    private static final Long USER_ID = 10L;
    private static final Long ORG_ID = 100L;
    private static final String RAW_TOKEN = "abc123-enrollment-token";

    @BeforeEach
    void setUp() {
        useCase = new ValidateEnrollmentTokenUseCase(
                tokenRepository, porterRepository, organizationRepository);
    }

    @Test
    @DisplayName("Should validate a valid PENDING token and return porter + org info")
    void shouldValidateValidToken() {
        String tokenHash = ValidateEnrollmentTokenUseCase.hashToken(RAW_TOKEN);
        PorterEnrollmentToken token = buildToken(PorterEnrollmentTokenStatus.PENDING,
                Instant.now().plus(1, ChronoUnit.HOURS));
        Porter porter = Porter.builder()
                .id(USER_ID).names("Portería Principal")
                .porterType(PorterType.PORTERO_GENERAL)
                .organizationId(ORG_ID).build();
        Organization org = Organization.builder()
                .id(ORG_ID).name("Conjunto El Bosque").build();

        when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Mono.just(token));
        when(porterRepository.findByUserIdAndOrganizationId(USER_ID, ORG_ID))
                .thenReturn(Mono.just(porter));
        when(organizationRepository.findById(ORG_ID)).thenReturn(Mono.just(org));

        StepVerifier.create(useCase.execute(RAW_TOKEN))
                .assertNext(result -> {
                    assertThat(result.valid()).isTrue();
                    assertThat(result.porterId()).isEqualTo(USER_ID);
                    assertThat(result.porterName()).isEqualTo("Portería Principal");
                    assertThat(result.organizationName()).isEqualTo("Conjunto El Bosque");
                    assertThat(result.expiresAt()).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should reject null or blank token")
    void shouldRejectBlankToken() {
        StepVerifier.create(useCase.execute(""))
                .expectErrorMatches(e -> e instanceof BusinessException
                        && ((BusinessException) e).getErrorCode().equals("TOKEN_REQUIRED"))
                .verify();

        StepVerifier.create(useCase.execute(null))
                .expectErrorMatches(e -> e instanceof BusinessException
                        && ((BusinessException) e).getErrorCode().equals("TOKEN_REQUIRED"))
                .verify();
    }

    @Test
    @DisplayName("Should return NotFoundException for unknown token hash")
    void shouldRejectUnknownToken() {
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute("unknown-token"))
                .expectError(NotFoundException.class)
                .verify();
    }

    @Test
    @DisplayName("Should reject consumed token")
    void shouldRejectConsumedToken() {
        String tokenHash = ValidateEnrollmentTokenUseCase.hashToken(RAW_TOKEN);
        PorterEnrollmentToken token = buildToken(PorterEnrollmentTokenStatus.CONSUMED, null);

        when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Mono.just(token));

        StepVerifier.create(useCase.execute(RAW_TOKEN))
                .expectErrorMatches(e -> e instanceof BusinessException
                        && ((BusinessException) e).getErrorCode().equals("TOKEN_ALREADY_USED"))
                .verify();
    }

    @Test
    @DisplayName("Should reject revoked token")
    void shouldRejectRevokedToken() {
        String tokenHash = ValidateEnrollmentTokenUseCase.hashToken(RAW_TOKEN);
        PorterEnrollmentToken token = buildToken(PorterEnrollmentTokenStatus.REVOKED, null);

        when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Mono.just(token));

        StepVerifier.create(useCase.execute(RAW_TOKEN))
                .expectErrorMatches(e -> e instanceof BusinessException
                        && ((BusinessException) e).getErrorCode().equals("TOKEN_REVOKED"))
                .verify();
    }

    @Test
    @DisplayName("Should reject expired token")
    void shouldRejectExpiredToken() {
        String tokenHash = ValidateEnrollmentTokenUseCase.hashToken(RAW_TOKEN);
        PorterEnrollmentToken token = buildToken(PorterEnrollmentTokenStatus.PENDING,
                Instant.now().minus(1, ChronoUnit.HOURS));

        when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Mono.just(token));

        StepVerifier.create(useCase.execute(RAW_TOKEN))
                .expectErrorMatches(e -> e instanceof BusinessException
                        && ((BusinessException) e).getErrorCode().equals("TOKEN_EXPIRED"))
                .verify();
    }

    @Test
    @DisplayName("Should reject token with EXPIRED status")
    void shouldRejectTokenWithExpiredStatus() {
        String tokenHash = ValidateEnrollmentTokenUseCase.hashToken(RAW_TOKEN);
        PorterEnrollmentToken token = buildToken(PorterEnrollmentTokenStatus.EXPIRED,
                Instant.now().plus(1, ChronoUnit.HOURS));

        when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Mono.just(token));

        StepVerifier.create(useCase.execute(RAW_TOKEN))
                .expectErrorMatches(e -> e instanceof BusinessException
                        && ((BusinessException) e).getErrorCode().equals("TOKEN_EXPIRED"))
                .verify();
    }

    private PorterEnrollmentToken buildToken(PorterEnrollmentTokenStatus status, Instant expiresAt) {
        return PorterEnrollmentToken.builder()
                .id(1L)
                .userId(USER_ID)
                .organizationId(ORG_ID)
                .tokenHash(ValidateEnrollmentTokenUseCase.hashToken(RAW_TOKEN))
                .status(status)
                .expiresAt(expiresAt)
                .createdAt(Instant.now())
                .build();
    }
}
