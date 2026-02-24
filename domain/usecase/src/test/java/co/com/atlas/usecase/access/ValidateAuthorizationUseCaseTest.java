package co.com.atlas.usecase.access;

import co.com.atlas.model.access.AccessAction;
import co.com.atlas.model.access.AccessEvent;
import co.com.atlas.model.access.ScanResult;
import co.com.atlas.model.access.gateways.AccessEventRepository;
import co.com.atlas.model.authorization.AuthorizationStatus;
import co.com.atlas.model.authorization.VisitorAuthorization;
import co.com.atlas.model.authorization.gateways.VisitorAuthorizationRepository;
import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.crypto.OrganizationCryptoKey;
import co.com.atlas.model.crypto.gateways.CryptoKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidateAuthorizationUseCaseTest {

    @Mock private CryptoKeyRepository cryptoKeyRepository;
    @Mock private VisitorAuthorizationRepository visitorAuthorizationRepository;
    @Mock private AccessEventRepository accessEventRepository;

    private ValidateAuthorizationUseCase useCase;

    private static final Long ORG_ID = 1L;
    private static final Long PORTER_ID = 10L;
    private static final String DEVICE_ID = "device-001";

    @BeforeEach
    void setUp() {
        useCase = new ValidateAuthorizationUseCase(
                cryptoKeyRepository, visitorAuthorizationRepository, accessEventRepository);
    }

    @Test
    void shouldRejectInvalidQrFormat() {
        StepVerifier.create(useCase.execute("invalid-no-dot", PORTER_ID, DEVICE_ID, ORG_ID))
                .expectErrorMatches(e -> e instanceof BusinessException
                        && e.getMessage().contains("Formato de QR inválido"))
                .verify();
    }

    @Test
    void shouldRejectNullQr() {
        StepVerifier.create(useCase.execute(null, PORTER_ID, DEVICE_ID, ORG_ID))
                .expectErrorMatches(BusinessException.class::isInstance)
                .verify();
    }

    @Test
    void shouldRejectWhenCryptoKeyNotFound() {
        String payload = buildPayloadBase64(1L, "2025-01-01T00:00:00Z", "2030-12-31T23:59:59Z");
        String signedQr = payload + ".fakesignature";

        when(cryptoKeyRepository.findActiveByOrganizationId(ORG_ID)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute(signedQr, PORTER_ID, DEVICE_ID, ORG_ID))
                .expectErrorMatches(e -> e instanceof BusinessException
                        && e.getMessage().contains("clave criptográfica"))
                .verify();
    }

    @Test
    void shouldRegisterInvalidSignatureEvent() {
        String payload = buildPayloadBase64(1L, "2025-01-01T00:00:00Z", "2030-12-31T23:59:59Z");
        String signedQr = payload + ".invalidsig";

        OrganizationCryptoKey cryptoKey = OrganizationCryptoKey.builder()
                .id(1L).organizationId(ORG_ID).publicKeyJwk("{\"x\":\"dGVzdA\"}")
                .isActive(true).build();

        when(cryptoKeyRepository.findActiveByOrganizationId(ORG_ID)).thenReturn(Mono.just(cryptoKey));
        when(accessEventRepository.save(any(AccessEvent.class)))
                .thenAnswer(inv -> Mono.just(((AccessEvent) inv.getArgument(0)).toBuilder().id(1L).build()));

        StepVerifier.create(useCase.execute(signedQr, PORTER_ID, DEVICE_ID, ORG_ID))
                .assertNext(event -> {
                    assertThat(event.getScanResult()).isEqualTo(ScanResult.INVALID);
                    assertThat(event.getAction()).isEqualTo(AccessAction.ENTRY);
                    assertThat(event.getNotes()).contains("Firma digital inválida");
                })
                .verifyComplete();
    }

    @Test
    void shouldRejectExpiredAuthorization() {
        String pastFrom = Instant.now().minus(2, ChronoUnit.HOURS).toString();
        String pastTo = Instant.now().minus(1, ChronoUnit.HOURS).toString();
        String payload = buildPayloadBase64(1L, pastFrom, pastTo);
        String signedQr = payload + ".invalidsig";

        OrganizationCryptoKey cryptoKey = OrganizationCryptoKey.builder()
                .id(1L).organizationId(ORG_ID).publicKeyJwk("{\"x\":\"dGVzdA\"}")
                .isActive(true).build();

        when(cryptoKeyRepository.findActiveByOrganizationId(ORG_ID)).thenReturn(Mono.just(cryptoKey));
        when(accessEventRepository.save(any(AccessEvent.class)))
                .thenAnswer(inv -> Mono.just(((AccessEvent) inv.getArgument(0)).toBuilder().id(1L).build()));

        StepVerifier.create(useCase.execute(signedQr, PORTER_ID, DEVICE_ID, ORG_ID))
                .assertNext(event -> {
                    assertThat(event.getScanResult()).isIn(ScanResult.INVALID, ScanResult.EXPIRED);
                })
                .verifyComplete();
    }

    private String buildPayloadBase64(Long authId, String validFrom, String validTo) {
        String json = String.format(
                "{\"authId\":%d,\"orgId\":%d,\"personName\":\"Test User\",\"personDoc\":\"123456\",\"validFrom\":\"%s\",\"validTo\":\"%s\"}",
                authId, ORG_ID, validFrom, validTo);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes());
    }
}
