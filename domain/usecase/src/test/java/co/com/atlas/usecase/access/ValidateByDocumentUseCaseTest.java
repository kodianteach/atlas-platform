package co.com.atlas.usecase.access;

import co.com.atlas.model.access.AccessEvent;
import co.com.atlas.model.access.ScanResult;
import co.com.atlas.model.access.gateways.AccessEventRepository;
import co.com.atlas.model.authorization.AuthorizationStatus;
import co.com.atlas.model.authorization.VisitorAuthorization;
import co.com.atlas.model.authorization.gateways.VisitorAuthorizationRepository;
import co.com.atlas.model.common.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidateByDocumentUseCaseTest {

    @Mock private VisitorAuthorizationRepository visitorAuthorizationRepository;
    @Mock private AccessEventRepository accessEventRepository;

    private ValidateByDocumentUseCase useCase;
    private static final Long ORG_ID = 1L;

    @BeforeEach
    void setUp() {
        useCase = new ValidateByDocumentUseCase(visitorAuthorizationRepository, accessEventRepository);
    }

    @Test
    void shouldFindActiveAuthorizationsByDocument() {
        Instant now = Instant.now();
        VisitorAuthorization activeAuth = VisitorAuthorization.builder()
                .id(1L).organizationId(ORG_ID).personDocument("123456")
                .personName("Juan Perez").status(AuthorizationStatus.ACTIVE)
                .validFrom(now.minus(1, ChronoUnit.HOURS))
                .validTo(now.plus(1, ChronoUnit.HOURS))
                .build();

        when(visitorAuthorizationRepository.findByOrganizationId(ORG_ID))
                .thenReturn(Flux.just(activeAuth));

        StepVerifier.create(useCase.findActiveByDocument("123456", ORG_ID))
                .assertNext(auth -> {
                    assertThat(auth.getPersonDocument()).isEqualTo("123456");
                    assertThat(auth.getStatus()).isEqualTo(AuthorizationStatus.ACTIVE);
                })
                .verifyComplete();
    }

    @Test
    void shouldFilterOutRevokedAuthorizations() {
        Instant now = Instant.now();
        VisitorAuthorization revokedAuth = VisitorAuthorization.builder()
                .id(1L).organizationId(ORG_ID).personDocument("123456")
                .status(AuthorizationStatus.REVOKED)
                .validFrom(now.minus(1, ChronoUnit.HOURS))
                .validTo(now.plus(1, ChronoUnit.HOURS))
                .build();

        when(visitorAuthorizationRepository.findByOrganizationId(ORG_ID))
                .thenReturn(Flux.just(revokedAuth));

        StepVerifier.create(useCase.findActiveByDocument("123456", ORG_ID))
                .verifyComplete();
    }

    @Test
    void shouldFilterOutExpiredAuthorizations() {
        Instant now = Instant.now();
        VisitorAuthorization expiredAuth = VisitorAuthorization.builder()
                .id(1L).organizationId(ORG_ID).personDocument("123456")
                .status(AuthorizationStatus.ACTIVE)
                .validFrom(now.minus(3, ChronoUnit.HOURS))
                .validTo(now.minus(2, ChronoUnit.HOURS))
                .build();

        when(visitorAuthorizationRepository.findByOrganizationId(ORG_ID))
                .thenReturn(Flux.just(expiredAuth));

        StepVerifier.create(useCase.findActiveByDocument("123456", ORG_ID))
                .verifyComplete();
    }

    @Test
    void shouldRejectEmptyDocument() {
        StepVerifier.create(useCase.findActiveByDocument("", ORG_ID))
                .expectErrorMatches(e -> e instanceof BusinessException
                        && e.getMessage().contains("documento"))
                .verify();
    }

    @Test
    void shouldIncludeUpcomingAuthorizationsInSearch() {
        Instant now = Instant.now();
        VisitorAuthorization upcomingAuth = VisitorAuthorization.builder()
                .id(2L).organizationId(ORG_ID).personDocument("123456")
                .personName("Carlos Gomez").status(AuthorizationStatus.ACTIVE)
                .validFrom(now.plus(12, ChronoUnit.HOURS))
                .validTo(now.plus(36, ChronoUnit.HOURS))
                .build();

        when(visitorAuthorizationRepository.findByOrganizationId(ORG_ID))
                .thenReturn(Flux.just(upcomingAuth));

        StepVerifier.create(useCase.findActiveByDocument("123456", ORG_ID))
                .assertNext(auth -> {
                    assertThat(auth.getPersonDocument()).isEqualTo("123456");
                    assertThat(auth.getPersonName()).isEqualTo("Carlos Gomez");
                })
                .verifyComplete();
    }

    @Test
    void shouldValidateAndRegisterEntry() {
        Instant now = Instant.now();
        VisitorAuthorization auth = VisitorAuthorization.builder()
                .id(5L).organizationId(ORG_ID).personDocument("123456")
                .personName("Ana Lopez").status(AuthorizationStatus.ACTIVE)
                .validFrom(now.minus(1, ChronoUnit.HOURS))
                .validTo(now.plus(1, ChronoUnit.HOURS))
                .build();

        when(visitorAuthorizationRepository.findById(5L)).thenReturn(Mono.just(auth));
        when(accessEventRepository.save(any(AccessEvent.class)))
                .thenAnswer(inv -> Mono.just(((AccessEvent) inv.getArgument(0)).toBuilder().id(1L).build()));

        StepVerifier.create(useCase.validateAndRegister(5L, 10L, "device-001", ORG_ID))
                .assertNext(event -> {
                    assertThat(event.getScanResult()).isEqualTo(ScanResult.VALID);
                    assertThat(event.getPersonName()).isEqualTo("Ana Lopez");
                    assertThat(event.getPersonDocument()).isEqualTo("123456");
                })
                .verifyComplete();
    }
}
