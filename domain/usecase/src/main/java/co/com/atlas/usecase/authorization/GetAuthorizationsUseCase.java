package co.com.atlas.usecase.authorization;

import co.com.atlas.model.authorization.VisitorAuthorization;
import co.com.atlas.model.authorization.gateways.VisitorAuthorizationRepository;
import co.com.atlas.model.userunit.gateways.UserUnitRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Caso de uso para consulta de autorizaciones con visibilidad por rol.
 * ADMIN_ATLAS → todas las de la organización.
 * OWNER → todas las de sus unidades.
 * TENANT/FAMILY → solo las que él creó.
 */
@RequiredArgsConstructor
public class GetAuthorizationsUseCase {

    private final VisitorAuthorizationRepository authorizationRepository;
    private final UserUnitRepository userUnitRepository;

    /**
     * Obtiene autorizaciones según el rol del usuario.
     *
     * @param userId         ID del usuario
     * @param organizationId ID de la organización
     * @param userRoles      Roles del usuario
     * @return Flux con las autorizaciones visibles para el usuario
     */
    public Flux<VisitorAuthorization> execute(Long userId, Long organizationId, List<String> userRoles) {
        if (userRoles.contains("ADMIN_ATLAS") || userRoles.contains("SUPER_ADMIN")) {
            return authorizationRepository.findByOrganizationId(organizationId);
        }

        if (userRoles.contains("OWNER")) {
            return userUnitRepository.findActiveByUserId(userId)
                    .flatMap(userUnit -> authorizationRepository.findByUnitId(userUnit.getUnitId()));
        }

        // TENANT / FAMILY: solo las que él creó
        return userUnitRepository.findPrimaryByUserId(userId)
                .flatMapMany(userUnit ->
                        authorizationRepository.findByUnitIdAndCreatedByUserId(
                                userUnit.getUnitId(), userId));
    }
}
