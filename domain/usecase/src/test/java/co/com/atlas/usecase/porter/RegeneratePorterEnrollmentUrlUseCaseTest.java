package co.com.atlas.usecase.porter;

import co.com.atlas.model.common.NotFoundException;
import co.com.atlas.model.porter.Porter;
import co.com.atlas.model.porter.PorterEnrollmentAuditLog;
import co.com.atlas.model.porter.PorterEnrollmentToken;
import co.com.atlas.model.porter.PorterEnrollmentTokenStatus;
import co.com.atlas.model.porter.PorterType;
import co.com.atlas.model.porter.gateways.PorterEnrollmentAuditRepository;
import co.com.atlas.model.porter.gateways.PorterEnrollmentTokenRepository;
import co.com.atlas.model.porter.gateways.PorterRepository;
import co.com.atlas.usecase.porter.builders.PorterEnrollmentTokenBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegeneratePorterEnrollmentUrlUseCaseTest {

    @Mock private PorterRepository porterRepository;
    @Mock private PorterEnrollmentTokenRepository tokenRepository;
    @Mock private PorterEnrollmentAuditRepository auditRepository;

    private RegeneratePorterEnrollmentUrlUseCase useCase;

    private static final Long ORG_ID = 100L;
    private static final Long ADMIN_USER_ID = 5L;
    private static final Long PORTER_USER_ID = 10L;

    @BeforeEach
    void setUp() {
        useCase = new RegeneratePorterEnrollmentUrlUseCase(porterRepository, tokenRepository, auditRepository);
    }

    @Test
    void shouldRegenerateUrlSuccessfullyWithPreviousToken() {
        Porter porter = Porter.builder().id(PORTER_USER_ID)
                .organizationId(ORG_ID).porterType(PorterType.PORTERO_GENERAL).build();
        PorterEnrollmentToken activeToken = PorterEnrollmentTokenBuilder.aToken()
                .withUserId(PORTER_USER_ID).withStatus(PorterEnrollmentTokenStatus.PENDING).build();
        PorterEnrollmentToken savedNewToken = PorterEnrollmentTokenBuilder.aToken()
                .withId(2L).withUserId(PORTER_USER_ID).build();

        when(porterRepository.findByUserIdAndOrganizationId(PORTER_USER_ID, ORG_ID)).thenReturn(Mono.just(porter));
        when(tokenRepository.findActiveByUserId(PORTER_USER_ID)).thenReturn(Mono.just(activeToken));
        when(tokenRepository.save(any(PorterEnrollmentToken.class))).thenReturn(Mono.just(savedNewToken));
        when(auditRepository.save(any(PorterEnrollmentAuditLog.class)))
                .thenReturn(Mono.just(PorterEnrollmentAuditLog.builder().id(1L).build()));

        StepVerifier.create(useCase.execute(PORTER_USER_ID, ORG_ID, ADMIN_USER_ID))
                .assertNext(result -> {
                    assertThat(result.enrollmentUrl()).startsWith("/porter-enroll?token=");
                })
                .verifyComplete();

        // Verify token was saved twice: 1 revoke + 1 new
        verify(tokenRepository, times(2)).save(any(PorterEnrollmentToken.class));
    }

    @Test
    void shouldRegenerateUrlWithoutPreviousActiveToken() {
        Porter porter = Porter.builder().id(PORTER_USER_ID)
                .organizationId(ORG_ID).porterType(PorterType.PORTERO_GENERAL).build();
        PorterEnrollmentToken savedNewToken = PorterEnrollmentTokenBuilder.aToken()
                .withId(2L).withUserId(PORTER_USER_ID).build();

        when(porterRepository.findByUserIdAndOrganizationId(PORTER_USER_ID, ORG_ID)).thenReturn(Mono.just(porter));
        when(tokenRepository.findActiveByUserId(PORTER_USER_ID)).thenReturn(Mono.empty());
        when(tokenRepository.save(any(PorterEnrollmentToken.class))).thenReturn(Mono.just(savedNewToken));
        when(auditRepository.save(any(PorterEnrollmentAuditLog.class)))
                .thenReturn(Mono.just(PorterEnrollmentAuditLog.builder().id(1L).build()));

        StepVerifier.create(useCase.execute(PORTER_USER_ID, ORG_ID, ADMIN_USER_ID))
                .assertNext(result -> {
                    assertThat(result.enrollmentUrl()).contains("token=");
                })
                .verifyComplete();

        // Only 1 save (new token, no revoke)
        verify(tokenRepository, times(1)).save(any(PorterEnrollmentToken.class));
    }

    @Test
    void shouldFailWhenPorterNotFound() {
        when(porterRepository.findByUserIdAndOrganizationId(999L, ORG_ID)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute(999L, ORG_ID, ADMIN_USER_ID))
                .expectError(NotFoundException.class)
                .verify();
    }

    @Test
    void shouldRevokeActiveTokenBeforeGeneratingNew() {
        Porter porter = Porter.builder().id(PORTER_USER_ID)
                .organizationId(ORG_ID).porterType(PorterType.PORTERO_GENERAL).build();
        PorterEnrollmentToken activeToken = PorterEnrollmentTokenBuilder.aToken()
                .withUserId(PORTER_USER_ID).withStatus(PorterEnrollmentTokenStatus.PENDING).build();
        PorterEnrollmentToken savedToken = PorterEnrollmentTokenBuilder.aToken()
                .withId(2L).withUserId(PORTER_USER_ID).build();

        ArgumentCaptor<PorterEnrollmentToken> tokenCaptor = ArgumentCaptor.forClass(PorterEnrollmentToken.class);
        when(porterRepository.findByUserIdAndOrganizationId(PORTER_USER_ID, ORG_ID)).thenReturn(Mono.just(porter));
        when(tokenRepository.findActiveByUserId(PORTER_USER_ID)).thenReturn(Mono.just(activeToken));
        when(tokenRepository.save(tokenCaptor.capture())).thenReturn(Mono.just(savedToken));
        when(auditRepository.save(any())).thenReturn(Mono.just(PorterEnrollmentAuditLog.builder().id(1L).build()));

        StepVerifier.create(useCase.execute(PORTER_USER_ID, ORG_ID, ADMIN_USER_ID)).expectNextCount(1).verifyComplete();

        // First save should be the revoked token
        PorterEnrollmentToken firstSaved = tokenCaptor.getAllValues().get(0);
        assertThat(firstSaved.getStatus()).isEqualTo(PorterEnrollmentTokenStatus.REVOKED);
    }
}
