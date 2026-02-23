package co.com.atlas.r2dbc.access;

import co.com.atlas.model.access.AccessAction;
import co.com.atlas.model.access.AccessEvent;
import co.com.atlas.model.access.ScanResult;
import co.com.atlas.model.access.gateways.AccessEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

/**
 * Implementación del gateway AccessEventRepository usando R2DBC.
 */
@Repository
@RequiredArgsConstructor
public class AccessEventRepositoryAdapter implements AccessEventRepository {

    private final AccessEventReactiveRepository repository;

    @Override
    public Mono<AccessEvent> save(AccessEvent accessEvent) {
        AccessEventEntity entity = toEntity(accessEvent);
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        return repository.save(entity).map(this::toDomain);
    }

    @Override
    public Flux<AccessEvent> saveBatch(List<AccessEvent> events) {
        Instant now = Instant.now();
        List<AccessEventEntity> entities = events.stream()
                .map(this::toEntity)
                .map(e -> e.getCreatedAt() == null ? e.toBuilder().createdAt(now).build() : e)
                .toList();
        return repository.saveAll(entities).map(this::toDomain);
    }

    @Override
    public Flux<AccessEvent> findByOrganizationId(Long organizationId) {
        return repository.findByOrganizationId(organizationId).map(this::toDomain);
    }

    @Override
    public Flux<AccessEvent> findByAuthorizationId(Long authorizationId) {
        return repository.findByAuthorizationId(authorizationId).map(this::toDomain);
    }

    @Override
    public Flux<AccessEvent> findByPorterUserId(Long porterUserId) {
        return repository.findByPorterUserId(porterUserId).map(this::toDomain);
    }

    @Override
    public Flux<AccessEvent> findByOrganizationIdAndFilters(Long organizationId, Instant from, Instant to,
                                                             AccessAction action, ScanResult scanResult) {
        return repository.findByOrganizationId(organizationId)
                .map(this::toDomain)
                .filter(event -> from == null || !event.getScannedAt().isBefore(from))
                .filter(event -> to == null || !event.getScannedAt().isAfter(to))
                .filter(event -> action == null || event.getAction() == action)
                .filter(event -> scanResult == null || event.getScanResult() == scanResult);
    }

    private AccessEvent toDomain(AccessEventEntity entity) {
        return AccessEvent.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .authorizationId(entity.getAuthorizationId())
                .porterUserId(entity.getPorterUserId())
                .deviceId(entity.getDeviceId())
                .action(entity.getAction() != null ? AccessAction.valueOf(entity.getAction()) : null)
                .scanResult(entity.getScanResult() != null ? ScanResult.valueOf(entity.getScanResult()) : null)
                .personName(entity.getPersonName())
                .personDocument(entity.getPersonDocument())
                .vehiclePlate(entity.getVehiclePlate())
                .vehicleMatch(entity.getVehicleMatch())
                .offlineValidated(entity.isOfflineValidated())
                .notes(entity.getNotes())
                .scannedAt(entity.getScannedAt())
                .syncedAt(entity.getSyncedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private AccessEventEntity toEntity(AccessEvent event) {
        return AccessEventEntity.builder()
                .id(event.getId())
                .organizationId(event.getOrganizationId())
                .authorizationId(event.getAuthorizationId())
                .porterUserId(event.getPorterUserId())
                .deviceId(event.getDeviceId())
                .action(event.getAction() != null ? event.getAction().name() : null)
                .scanResult(event.getScanResult() != null ? event.getScanResult().name() : null)
                .personName(event.getPersonName())
                .personDocument(event.getPersonDocument())
                .vehiclePlate(event.getVehiclePlate())
                .vehicleMatch(event.getVehicleMatch())
                .offlineValidated(event.isOfflineValidated())
                .notes(event.getNotes())
                .scannedAt(event.getScannedAt())
                .syncedAt(event.getSyncedAt())
                .createdAt(event.getCreatedAt())
                .build();
    }
}
