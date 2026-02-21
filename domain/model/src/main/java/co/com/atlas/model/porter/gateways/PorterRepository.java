package co.com.atlas.model.porter.gateways;

import co.com.atlas.model.porter.Porter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Gateway de lectura para porteros.
 * Un portero es un usuario con rol PORTERO_GENERAL o PORTERO_DELIVERY.
 * No tiene tabla propia; se consulta via JOIN de users + user_roles_multi + role.
 */
public interface PorterRepository {

    /**
     * Busca todos los porteros de una organización.
     */
    Flux<Porter> findByOrganizationId(Long organizationId);

    /**
     * Busca un portero por user ID y organización.
     */
    Mono<Porter> findByUserIdAndOrganizationId(Long userId, Long organizationId);
}
