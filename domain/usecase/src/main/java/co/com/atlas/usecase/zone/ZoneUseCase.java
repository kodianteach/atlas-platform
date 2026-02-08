package co.com.atlas.usecase.zone;

import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.common.DuplicateException;
import co.com.atlas.model.common.NotFoundException;
import co.com.atlas.model.organization.gateways.OrganizationRepository;
import co.com.atlas.model.zone.Zone;
import co.com.atlas.model.zone.gateways.ZoneRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Caso de uso para gestión de zonas.
 */
@RequiredArgsConstructor
public class ZoneUseCase {
    
    private final ZoneRepository zoneRepository;
    private final OrganizationRepository organizationRepository;
    
    /**
     * Crea una nueva zona.
     */
    public Mono<Zone> create(Zone zone) {
        return organizationRepository.findById(zone.getOrganizationId())
                .switchIfEmpty(Mono.error(new NotFoundException("Organization", zone.getOrganizationId())))
                .flatMap(org -> {
                    if (Boolean.FALSE.equals(org.getUsesZones())) {
                        return Mono.error(new BusinessException(
                                "Esta organización no utiliza zonas", "ZONES_NOT_ENABLED"));
                    }
                    return zoneRepository.existsByOrganizationIdAndCode(zone.getOrganizationId(), zone.getCode())
                            .flatMap(exists -> {
                                if (Boolean.TRUE.equals(exists)) {
                                    return Mono.error(new DuplicateException("Zone", "code", zone.getCode()));
                                }
                                Zone newZone = zone.toBuilder()
                                        .isActive(true)
                                        .sortOrder(zone.getSortOrder() != null ? zone.getSortOrder() : 0)
                                        .build();
                                return zoneRepository.save(newZone);
                            });
                });
    }
    
    /**
     * Obtiene una zona por ID.
     */
    public Mono<Zone> findById(Long id) {
        return zoneRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Zone", id)));
    }
    
    /**
     * Lista las zonas de una organización.
     */
    public Flux<Zone> findByOrganizationId(Long organizationId) {
        return zoneRepository.findByOrganizationId(organizationId);
    }
    
    /**
     * Lista las zonas activas de una organización.
     */
    public Flux<Zone> findActiveByOrganizationId(Long organizationId) {
        return zoneRepository.findActiveByOrganizationId(organizationId);
    }
    
    /**
     * Actualiza una zona.
     */
    public Mono<Zone> update(Long id, Zone zone) {
        return findById(id)
                .flatMap(existing -> {
                    Zone updated = existing.toBuilder()
                            .name(zone.getName() != null ? zone.getName() : existing.getName())
                            .description(zone.getDescription() != null ? zone.getDescription() : existing.getDescription())
                            .sortOrder(zone.getSortOrder() != null ? zone.getSortOrder() : existing.getSortOrder())
                            .isActive(zone.getIsActive() != null ? zone.getIsActive() : existing.getIsActive())
                            .build();
                    return zoneRepository.save(updated);
                });
    }
    
    /**
     * Elimina una zona (soft delete).
     */
    public Mono<Void> delete(Long id) {
        return findById(id)
                .flatMap(existing -> zoneRepository.delete(id));
    }
}
