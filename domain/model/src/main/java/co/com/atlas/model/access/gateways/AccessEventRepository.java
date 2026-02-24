package co.com.atlas.model.access.gateways;

import co.com.atlas.model.access.AccessAction;
import co.com.atlas.model.access.AccessEvent;
import co.com.atlas.model.access.ScanResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

/**
 * Gateway de dominio para operaciones de eventos de acceso.
 */
public interface AccessEventRepository {

    /**
     * Guarda un evento de acceso.
     */
    Mono<AccessEvent> save(AccessEvent accessEvent);

    /**
     * Guarda un lote de eventos (sincronización offline).
     */
    Flux<AccessEvent> saveBatch(List<AccessEvent> events);

    /**
     * Lista eventos por organización.
     */
    Flux<AccessEvent> findByOrganizationId(Long organizationId);

    /**
     * Lista eventos por autorización.
     */
    Flux<AccessEvent> findByAuthorizationId(Long authorizationId);

    /**
     * Lista eventos por portero.
     */
    Flux<AccessEvent> findByPorterUserId(Long porterUserId);

    /**
     * Lista eventos filtrados por organización, rango de fechas, acción y resultado.
     */
    Flux<AccessEvent> findByOrganizationIdAndFilters(Long organizationId, Instant from, Instant to,
                                                      AccessAction action, ScanResult scanResult);
}
