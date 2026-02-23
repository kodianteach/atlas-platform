package co.com.atlas.usecase.porter;

import co.com.atlas.model.auth.AuthUser;
import co.com.atlas.model.auth.gateways.AuthUserRepository;
import co.com.atlas.model.common.NotFoundException;
import co.com.atlas.model.porter.gateways.PorterRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Caso de uso para activar/inactivar un portero.
 * Cambia el campo active del usuario portero (true ↔ false).
 */
@RequiredArgsConstructor
public class TogglePorterStatusUseCase {

    private final PorterRepository porterRepository;
    private final AuthUserRepository authUserRepository;

    /**
     * Resultado de la operación de toggle.
     */
    public record ToggleResult(Long porterId, boolean active, String status) {}

    /**
     * Activa o inactiva un portero.
     *
     * @param porterUserId   ID del usuario portero
     * @param organizationId ID de la organización del admin
     * @return Resultado con el nuevo estado
     */
    public Mono<ToggleResult> execute(Long porterUserId, Long organizationId) {
        return porterRepository.findByUserIdAndOrganizationId(porterUserId, organizationId)
                .switchIfEmpty(Mono.error(new NotFoundException(
                        "Portero no encontrado en esta organización")))
                .flatMap(porter -> authUserRepository.findById(porter.getId())
                        .switchIfEmpty(Mono.error(new NotFoundException("Usuario portero no encontrado")))
                        .flatMap(user -> {
                            boolean newActive = !user.isActive();
                            AuthUser updated = user.toBuilder()
                                    .active(newActive)
                                    .build();
                            return authUserRepository.save(updated)
                                    .map(saved -> new ToggleResult(
                                            saved.getId(),
                                            saved.isActive(),
                                            saved.isActive() ? "ACTIVE" : "INACTIVE"
                                    ));
                        }));
    }
}
