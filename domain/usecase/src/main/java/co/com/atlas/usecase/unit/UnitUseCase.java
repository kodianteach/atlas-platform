package co.com.atlas.usecase.unit;

import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.common.DuplicateException;
import co.com.atlas.model.common.NotFoundException;
import co.com.atlas.model.organization.OrganizationType;
import co.com.atlas.model.organization.gateways.OrganizationRepository;
import co.com.atlas.model.unit.Unit;
import co.com.atlas.model.unit.UnitStatus;
import co.com.atlas.model.unit.UnitType;
import co.com.atlas.model.unit.gateways.UnitRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Caso de uso para gestión de unidades habitacionales.
 */
@RequiredArgsConstructor
public class UnitUseCase {
    
    private final UnitRepository unitRepository;
    private final OrganizationRepository organizationRepository;
    
    /**
     * Crea una nueva unidad.
     */
    public Mono<Unit> create(Unit unit) {
        return organizationRepository.findById(unit.getOrganizationId())
                .switchIfEmpty(Mono.error(new NotFoundException("Organization", unit.getOrganizationId())))
                .flatMap(org -> {
                    // Validaciones según tipo de organización
                    if (org.getType() == OrganizationType.CIUDADELA && unit.getType() == UnitType.HOUSE) {
                        return Mono.error(new BusinessException(
                                "Las casas no están permitidas en organizaciones tipo CIUDADELA",
                                "INVALID_UNIT_TYPE"));
                    }
                    if (org.getType() == OrganizationType.CONJUNTO && unit.getType() == UnitType.APARTMENT) {
                        return Mono.error(new BusinessException(
                                "Los apartamentos no están permitidos en organizaciones tipo CONJUNTO",
                                "INVALID_UNIT_TYPE"));
                    }
                    if (unit.getType() == UnitType.APARTMENT && unit.getTowerId() == null) {
                        return Mono.error(new BusinessException(
                                "Los apartamentos requieren una torre asociada",
                                "TOWER_REQUIRED"));
                    }
                    if (unit.getType() == UnitType.HOUSE && unit.getFloor() != null) {
                        return Mono.error(new BusinessException(
                                "Las casas no pueden tener piso asignado",
                                "INVALID_FLOOR"));
                    }
                    
                    return unitRepository.existsByOrganizationIdAndCode(unit.getOrganizationId(), unit.getCode())
                            .flatMap(exists -> {
                                if (Boolean.TRUE.equals(exists)) {
                                    return Mono.error(new DuplicateException("Unit", "code", unit.getCode()));
                                }
                                Unit newUnit = unit.toBuilder()
                                        .status(UnitStatus.AVAILABLE)
                                        .isActive(true)
                                        .build();
                                return unitRepository.save(newUnit);
                            });
                });
    }
    
    /**
     * Obtiene una unidad por ID.
     */
    public Mono<Unit> findById(Long id) {
        return unitRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Unit", id)));
    }
    
    /**
     * Lista las unidades de una organización.
     */
    public Flux<Unit> findByOrganizationId(Long organizationId) {
        return unitRepository.findByOrganizationId(organizationId);
    }
    
    /**
     * Lista las unidades de una zona.
     */
    public Flux<Unit> findByZoneId(Long zoneId) {
        return unitRepository.findByZoneId(zoneId);
    }
    
    /**
     * Lista las unidades de una torre.
     */
    public Flux<Unit> findByTowerId(Long towerId) {
        return unitRepository.findByTowerId(towerId);
    }
    
    /**
     * Lista las unidades activas de una organización.
     */
    public Flux<Unit> findActiveByOrganizationId(Long organizationId) {
        return unitRepository.findByOrganizationId(organizationId)
                .filter(unit -> Boolean.TRUE.equals(unit.getIsActive()));
    }
    
    /**
     * Actualiza una unidad.
     */
    public Mono<Unit> update(Long id, Unit unit) {
        return findById(id)
                .flatMap(existing -> {
                    Unit updated = existing.toBuilder()
                            .areaSqm(unit.getAreaSqm() != null ? unit.getAreaSqm() : existing.getAreaSqm())
                            .bedrooms(unit.getBedrooms() != null ? unit.getBedrooms() : existing.getBedrooms())
                            .bathrooms(unit.getBathrooms() != null ? unit.getBathrooms() : existing.getBathrooms())
                            .parkingSpots(unit.getParkingSpots() != null ? unit.getParkingSpots() : existing.getParkingSpots())
                            .isActive(unit.getIsActive() != null ? unit.getIsActive() : existing.getIsActive())
                            .build();
                    return unitRepository.save(updated);
                });
    }
    
    /**
     * Actualiza el estado de una unidad.
     */
    public Mono<Void> updateStatus(Long id, UnitStatus status) {
        return findById(id)
                .flatMap(existing -> {
                    Unit updated = existing.toBuilder().status(status).build();
                    return unitRepository.save(updated).then();
                });
    }
    
    /**
     * Elimina una unidad (soft delete).
     */
    public Mono<Void> delete(Long id) {
        return findById(id)
                .flatMap(existing -> unitRepository.delete(id));
    }
}
