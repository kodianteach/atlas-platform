package co.com.atlas.r2dbc.authorization;

import co.com.atlas.model.authorization.AuthorizationStatus;
import co.com.atlas.model.authorization.ServiceType;
import co.com.atlas.model.authorization.VisitorAuthorization;
import co.com.atlas.model.authorization.gateways.VisitorAuthorizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Implementación del gateway VisitorAuthorizationRepository usando R2DBC.
 */
@Repository
@RequiredArgsConstructor
public class VisitorAuthorizationRepositoryAdapter implements VisitorAuthorizationRepository {

    private final VisitorAuthorizationReactiveRepository repository;
    private final DatabaseClient databaseClient;

    @Override
    public Mono<VisitorAuthorization> save(VisitorAuthorization authorization) {
        VisitorAuthorizationEntity entity = toEntity(authorization);
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        entity.setUpdatedAt(Instant.now());
        return repository.save(entity)
                .map(this::toDomain);
    }

    @Override
    public Mono<VisitorAuthorization> findById(Long id) {
        return repository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public Flux<VisitorAuthorization> findByOrganizationId(Long organizationId) {
        return repository.findByOrganizationIdOrderByCreatedAtDesc(organizationId)
                .map(this::toDomain);
    }

    @Override
    public Flux<VisitorAuthorization> findByUnitId(Long unitId) {
        return repository.findByUnitIdOrderByCreatedAtDesc(unitId)
                .map(this::toDomain);
    }

    @Override
    public Flux<VisitorAuthorization> findByUnitIdAndCreatedByUserId(Long unitId, Long userId) {
        return repository.findByUnitIdAndCreatedByUserIdOrderByCreatedAtDesc(unitId, userId)
                .map(this::toDomain);
    }

    @Override
    public Flux<VisitorAuthorization> findByCreatedByUserId(Long userId) {
        return repository.findByCreatedByUserIdOrderByCreatedAtDesc(userId)
                .map(this::toDomain);
    }

    @Override
    public Mono<VisitorAuthorization> updateStatus(Long id, AuthorizationStatus status, Long revokedBy) {
        String sql = """
            UPDATE visitor_authorizations 
            SET status = :status, revoked_at = :revokedAt, revoked_by = :revokedBy, updated_at = :updatedAt
            WHERE id = :id
            """;
        Instant now = Instant.now();
        return databaseClient.sql(sql)
                .bind("status", status.name())
                .bind("revokedAt", now)
                .bind("revokedBy", revokedBy)
                .bind("updatedAt", now)
                .bind("id", id)
                .fetch()
                .rowsUpdated()
                .then(findById(id));
    }

    // ===================== Mappers =====================

    private VisitorAuthorization toDomain(VisitorAuthorizationEntity entity) {
        return VisitorAuthorization.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .unitId(entity.getUnitId())
                .createdByUserId(entity.getCreatedByUserId())
                .personName(entity.getPersonName())
                .personDocument(entity.getPersonDocument())
                .serviceType(entity.getServiceType() != null
                        ? ServiceType.valueOf(entity.getServiceType()) : null)
                .validFrom(entity.getValidFrom())
                .validTo(entity.getValidTo())
                .vehiclePlate(entity.getVehiclePlate())
                .vehicleType(entity.getVehicleType())
                .vehicleColor(entity.getVehicleColor())
                .identityDocumentKey(entity.getIdentityDocumentKey())
                .signedQr(entity.getSignedQr())
                .status(entity.getStatus() != null
                        ? AuthorizationStatus.valueOf(entity.getStatus()) : null)
                .revokedAt(entity.getRevokedAt())
                .revokedBy(entity.getRevokedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private VisitorAuthorizationEntity toEntity(VisitorAuthorization domain) {
        return VisitorAuthorizationEntity.builder()
                .id(domain.getId())
                .organizationId(domain.getOrganizationId())
                .unitId(domain.getUnitId())
                .createdByUserId(domain.getCreatedByUserId())
                .personName(domain.getPersonName())
                .personDocument(domain.getPersonDocument())
                .serviceType(domain.getServiceType() != null
                        ? domain.getServiceType().name() : null)
                .validFrom(domain.getValidFrom())
                .validTo(domain.getValidTo())
                .vehiclePlate(domain.getVehiclePlate())
                .vehicleType(domain.getVehicleType())
                .vehicleColor(domain.getVehicleColor())
                .identityDocumentKey(domain.getIdentityDocumentKey())
                .signedQr(domain.getSignedQr())
                .status(domain.getStatus() != null ? domain.getStatus().name() : null)
                .revokedAt(domain.getRevokedAt())
                .revokedBy(domain.getRevokedBy())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
