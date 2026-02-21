package co.com.atlas.usecase.authorization;

import co.com.atlas.model.authorization.VisitorAuthorization;
import co.com.atlas.model.authorization.gateways.VisitorAuthorizationRepository;
import co.com.atlas.model.common.NotFoundException;
import co.com.atlas.model.common.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Caso de uso para consulta individual de una autorización con validación de acceso.
 */
@RequiredArgsConstructor
public class GetAuthorizationByIdUseCase {

    private final VisitorAuthorizationRepository authorizationRepository;

    /**
     * Obtiene una autorización por ID validando que el usuario tenga acceso.
     *
     * @param authorizationId ID de la autorización
     * @param userId          ID del usuario solicitante
     * @param organizationId  ID de la organización del contexto
     * @param userRoles       Roles del usuario
     * @return Mono con la autorización
     */
    public Mono<VisitorAuthorization> execute(Long authorizationId, Long userId,
                                               Long organizationId, List<String> userRoles) {
        return authorizationRepository.findById(authorizationId)
                .switchIfEmpty(Mono.error(new NotFoundException("Autorización", authorizationId)))
                .flatMap(authorization -> validateAccess(authorization, userId, organizationId, userRoles));
    }

    private Mono<VisitorAuthorization> validateAccess(VisitorAuthorization authorization,
                                                       Long userId,
                                                       Long organizationId,
                                                       List<String> userRoles) {
        // Validar que pertenece a la misma organización
        if (!authorization.getOrganizationId().equals(organizationId)) {
            return Mono.error(new UnauthorizedException(
                    "No tiene acceso a autorizaciones de otra organización"));
        }

        // ADMIN_ATLAS ve todo de su organización
        if (userRoles.contains("ADMIN_ATLAS") || userRoles.contains("SUPER_ADMIN")) {
            return Mono.just(authorization);
        }

        // OWNER ve todo de su unidad (verificación de unidad se hace en GetAuthorizationsUseCase)
        if (userRoles.contains("OWNER")) {
            return Mono.just(authorization);
        }

        // TENANT/FAMILY solo ven las que ellos crearon
        if (authorization.getCreatedByUserId().equals(userId)) {
            return Mono.just(authorization);
        }

        return Mono.error(new UnauthorizedException(
                "No tiene acceso a esta autorización"));
    }
}
