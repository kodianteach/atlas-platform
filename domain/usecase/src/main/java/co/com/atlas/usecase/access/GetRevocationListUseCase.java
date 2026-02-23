package co.com.atlas.usecase.access;

import co.com.atlas.model.authorization.AuthorizationStatus;
import co.com.atlas.model.authorization.VisitorAuthorization;
import co.com.atlas.model.authorization.gateways.VisitorAuthorizationRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

import java.time.Instant;

/**
 * Caso de uso para obtener la lista de autorizaciones revocadas.
 * El dispositivo de portería consulta periódicamente para mantener
 * actualizada su cache de revocaciones offline.
 */
@RequiredArgsConstructor
public class GetRevocationListUseCase {

    private final VisitorAuthorizationRepository visitorAuthorizationRepository;

    /**
     * Obtiene IDs de autorizaciones revocadas desde una fecha.
     *
     * @param organizationId ID de la organización
     * @param since          Timestamp desde el cual buscar revocaciones
     * @return IDs de autorizaciones revocadas
     */
    public Flux<Long> execute(Long organizationId, Instant since) {
        return visitorAuthorizationRepository.findByOrganizationId(organizationId)
                .filter(auth -> auth.getStatus() == AuthorizationStatus.REVOKED)
                .filter(auth -> since == null || auth.getRevokedAt() == null || !auth.getRevokedAt().isBefore(since))
                .map(VisitorAuthorization::getId);
    }
}
