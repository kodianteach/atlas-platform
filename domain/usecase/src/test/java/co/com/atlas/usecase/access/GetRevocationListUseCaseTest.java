package co.com.atlas.usecase.access;

import co.com.atlas.model.authorization.AuthorizationStatus;
import co.com.atlas.model.authorization.VisitorAuthorization;
import co.com.atlas.model.authorization.gateways.VisitorAuthorizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetRevocationListUseCaseTest {

    @Mock private VisitorAuthorizationRepository visitorAuthorizationRepository;

    private GetRevocationListUseCase useCase;
    private static final Long ORG_ID = 1L;

    @BeforeEach
    void setUp() {
        useCase = new GetRevocationListUseCase(visitorAuthorizationRepository);
    }

    @Test
    void shouldReturnEmptyWhenNoRevocations() {
        when(visitorAuthorizationRepository.findByOrganizationId(ORG_ID))
                .thenReturn(Flux.just(
                        VisitorAuthorization.builder().id(1L).status(AuthorizationStatus.ACTIVE).build()
                ));

        StepVerifier.create(useCase.execute(ORG_ID, null))
                .verifyComplete();
    }

    @Test
    void shouldReturnRevokedAuthorizationIds() {
        Instant now = Instant.now();
        when(visitorAuthorizationRepository.findByOrganizationId(ORG_ID))
                .thenReturn(Flux.just(
                        VisitorAuthorization.builder().id(1L).status(AuthorizationStatus.REVOKED).revokedAt(now).build(),
                        VisitorAuthorization.builder().id(2L).status(AuthorizationStatus.ACTIVE).build(),
                        VisitorAuthorization.builder().id(3L).status(AuthorizationStatus.REVOKED).revokedAt(now).build()
                ));

        StepVerifier.create(useCase.execute(ORG_ID, null))
                .expectNext(1L)
                .expectNext(3L)
                .verifyComplete();
    }

    @Test
    void shouldFilterBySinceTimestamp() {
        Instant hourAgo = Instant.now().minusSeconds(3600);
        Instant dayAgo = Instant.now().minusSeconds(86400);
        Instant since = Instant.now().minusSeconds(7200);

        when(visitorAuthorizationRepository.findByOrganizationId(ORG_ID))
                .thenReturn(Flux.just(
                        VisitorAuthorization.builder().id(1L).status(AuthorizationStatus.REVOKED).revokedAt(hourAgo).build(),
                        VisitorAuthorization.builder().id(2L).status(AuthorizationStatus.REVOKED).revokedAt(dayAgo).build()
                ));

        StepVerifier.create(useCase.execute(ORG_ID, since))
                .expectNext(1L)
                .expectNext(2L)
                .verifyComplete();
    }
}
