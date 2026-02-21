package co.com.atlas.model.authorization.gateways;

import co.com.atlas.model.authorization.AuthorizationStatus;
import co.com.atlas.model.authorization.VisitorAuthorization;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Gateway de dominio para operaciones de autorización de visitantes.
 */
public interface VisitorAuthorizationRepository {

    /**
     * Guarda una nueva autorización.
     */
    Mono<VisitorAuthorization> save(VisitorAuthorization authorization);

    /**
     * Busca una autorización por ID.
     */
    Mono<VisitorAuthorization> findById(Long id);

    /**
     * Busca autorizaciones por organización (ADMIN_ATLAS).
     */
    Flux<VisitorAuthorization> findByOrganizationId(Long organizationId);

    /**
     * Busca autorizaciones por unidad (OWNER).
     */
    Flux<VisitorAuthorization> findByUnitId(Long unitId);

    /**
     * Busca autorizaciones por unidad y usuario creador (TENANT/FAMILY).
     */
    Flux<VisitorAuthorization> findByUnitIdAndCreatedByUserId(Long unitId, Long userId);

    /**
     * Busca autorizaciones creadas por un usuario específico.
     */
    Flux<VisitorAuthorization> findByCreatedByUserId(Long userId);

    /**
     * Actualiza el estado de una autorización.
     */
    Mono<VisitorAuthorization> updateStatus(Long id, AuthorizationStatus status, Long revokedBy);
}
