package co.com.atlas.model.vehicle.gateways;

import co.com.atlas.model.vehicle.Vehicle;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Gateway para operaciones de Vehicle.
 */
public interface VehicleRepository {

    /**
     * Busca un vehículo por ID (no eliminado).
     */
    Mono<Vehicle> findById(Long id);

    /**
     * Lista vehículos no eliminados de una unidad.
     */
    Flux<Vehicle> findByUnitId(Long unitId);

    /**
     * Lista vehículos activos y no eliminados de una unidad.
     */
    Flux<Vehicle> findActiveByUnitId(Long unitId);

    /**
     * Lista vehículos no eliminados de una organización (paginado).
     *
     * @param organizationId ID de la organización
     * @param page           Número de página (0-based)
     * @param size           Tamaño de página
     * @return Flux de vehículos en la página solicitada
     */
    Flux<Vehicle> findByOrganizationId(Long organizationId, int page, int size);

    /**
     * Cuenta el total de vehículos no eliminados de una organización.
     */
    Mono<Long> countByOrganizationId(Long organizationId);

    /**
     * Busca un vehículo por placa y organización (no eliminado).
     */
    Mono<Vehicle> findByOrganizationIdAndPlate(Long organizationId, String plate);

    /**
     * Cuenta vehículos activos y no eliminados de una unidad.
     */
    Mono<Long> countActiveByUnitId(Long unitId);

    /**
     * Guarda (crea o actualiza) un vehículo.
     */
    Mono<Vehicle> save(Vehicle vehicle);

    /**
     * Soft delete de un vehículo por ID.
     */
    Mono<Void> delete(Long id);

    /**
     * Inactiva todos los vehículos activos de una unidad.
     *
     * @return cantidad de filas afectadas
     */
    Mono<Long> inactivateAllByUnitId(Long unitId);

    /**
     * Verifica si existe un vehículo activo y no eliminado con la placa dada en la organización.
     */
    Mono<Boolean> existsActiveByOrganizationIdAndPlate(Long organizationId, String plate);
}
