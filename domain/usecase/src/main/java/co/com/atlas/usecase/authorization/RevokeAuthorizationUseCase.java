package co.com.atlas.usecase.authorization;

import co.com.atlas.model.authorization.AuthorizationStatus;
import co.com.atlas.model.authorization.VisitorAuthorization;
import co.com.atlas.model.authorization.gateways.VisitorAuthorizationRepository;
import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.common.NotFoundException;
import co.com.atlas.model.common.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

/**
 * Caso de uso para revocación de autorizaciones.
 * Solo permite revocar al creador o al ADMIN_ATLAS.
 */
@RequiredArgsConstructor
public class RevokeAuthorizationUseCase {

    private final VisitorAuthorizationRepository authorizationRepository;

    /**
     * Revoca una autorización activa.
     *
     * @param authorizationId ID de la autorización a revocar
     * @param userId          ID del usuario que solicita la revocación
     * @param userRoles       Roles del usuario en la organización
     * @return Mono con la autorización revocada
     */
    public Mono<VisitorAuthorization> execute(Long authorizationId, Long userId, List<String> userRoles) {
        return authorizationRepository.findById(authorizationId)
                .switchIfEmpty(Mono.error(new NotFoundException("Autorización", authorizationId)))
                .flatMap(authorization -> validateAndRevoke(authorization, userId, userRoles));
    }

    private Mono<VisitorAuthorization> validateAndRevoke(VisitorAuthorization authorization,
                                                          Long userId,
                                                          List<String> userRoles) {
        if (authorization.getStatus() != AuthorizationStatus.ACTIVE) {
            return Mono.error(new BusinessException(
                    "Solo se pueden revocar autorizaciones activas", "NOT_ACTIVE"));
        }

        boolean isCreator = authorization.getCreatedByUserId().equals(userId);
        boolean isAdmin = userRoles != null && userRoles.contains("ADMIN_ATLAS");

        if (!isCreator && !isAdmin) {
            return Mono.error(new UnauthorizedException(
                    "Solo el creador o el administrador pueden revocar esta autorización"));
        }

        return authorizationRepository.updateStatus(
                authorization.getId(),
                AuthorizationStatus.REVOKED,
                userId
        );
    }
}
