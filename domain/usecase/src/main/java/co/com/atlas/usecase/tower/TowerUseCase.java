package co.com.atlas.usecase.tower;

import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.common.DuplicateException;
import co.com.atlas.model.common.NotFoundException;
import co.com.atlas.model.organization.OrganizationType;
import co.com.atlas.model.organization.gateways.OrganizationRepository;
import co.com.atlas.model.tower.Tower;
import co.com.atlas.model.tower.gateways.TowerRepository;
import co.com.atlas.model.zone.gateways.ZoneRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Caso de uso para gestión de torres.
 */
@RequiredArgsConstructor
public class TowerUseCase {
    
    private final TowerRepository towerRepository;
    private final ZoneRepository zoneRepository;
    private final OrganizationRepository organizationRepository;
    
    /**
     * Crea una nueva torre.
     */
    public Mono<Tower> create(Tower tower) {
        return zoneRepository.findById(tower.getZoneId())
                .switchIfEmpty(Mono.error(new NotFoundException("Zone", tower.getZoneId())))
                .flatMap(zone -> organizationRepository.findById(zone.getOrganizationId())
                        .flatMap(org -> {
                            if (org.getType() != OrganizationType.CIUDADELA) {
                                return Mono.error(new BusinessException(
                                        "Las torres solo están disponibles en organizaciones tipo CIUDADELA",
                                        "TOWERS_NOT_ALLOWED"));
                            }
                            return towerRepository.existsByZoneIdAndCode(tower.getZoneId(), tower.getCode())
                                    .flatMap(exists -> {
                                        if (Boolean.TRUE.equals(exists)) {
                                            return Mono.error(new DuplicateException("Tower", "code", tower.getCode()));
                                        }
                                        Tower newTower = tower.toBuilder()
                                                .isActive(true)
                                                .sortOrder(tower.getSortOrder() != null ? tower.getSortOrder() : 0)
                                                .build();
                                        return towerRepository.save(newTower);
                                    });
                        }));
    }
    
    /**
     * Obtiene una torre por ID.
     */
    public Mono<Tower> findById(Long id) {
        return towerRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Tower", id)));
    }
    
    /**
     * Lista las torres de una zona.
     */
    public Flux<Tower> findByZoneId(Long zoneId) {
        return towerRepository.findByZoneId(zoneId);
    }
    
    /**
     * Lista las torres activas de una zona.
     */
    public Flux<Tower> findActiveByZoneId(Long zoneId) {
        return towerRepository.findActiveByZoneId(zoneId);
    }
    
    /**
     * Lista las torres de una organización.
     */
    public Flux<Tower> findByOrganizationId(Long organizationId) {
        return towerRepository.findByOrganizationId(organizationId);
    }
    
    /**
     * Actualiza una torre.
     */
    public Mono<Tower> update(Long id, Tower tower) {
        return findById(id)
                .flatMap(existing -> {
                    Tower updated = existing.toBuilder()
                            .name(tower.getName() != null ? tower.getName() : existing.getName())
                            .description(tower.getDescription() != null ? tower.getDescription() : existing.getDescription())
                            .floorsCount(tower.getFloorsCount() != null ? tower.getFloorsCount() : existing.getFloorsCount())
                            .sortOrder(tower.getSortOrder() != null ? tower.getSortOrder() : existing.getSortOrder())
                            .isActive(tower.getIsActive() != null ? tower.getIsActive() : existing.getIsActive())
                            .build();
                    return towerRepository.save(updated);
                });
    }
    
    /**
     * Elimina una torre (soft delete).
     */
    public Mono<Void> delete(Long id) {
        return findById(id)
                .flatMap(existing -> towerRepository.delete(id));
    }
}
