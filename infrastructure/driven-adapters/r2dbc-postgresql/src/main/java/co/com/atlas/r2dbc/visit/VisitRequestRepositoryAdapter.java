package co.com.atlas.r2dbc.visit;

import co.com.atlas.model.visit.RecurrenceType;
import co.com.atlas.model.visit.VisitRequest;
import co.com.atlas.model.visit.VisitStatus;
import co.com.atlas.model.visit.gateways.VisitRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Implementación del gateway VisitRequestRepository usando R2DBC.
 */
@Repository
@RequiredArgsConstructor
public class VisitRequestRepositoryAdapter implements VisitRequestRepository {

    private final VisitRequestReactiveRepository repository;

    @Override
    public Mono<VisitRequest> findById(Long id) {
        return repository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public Flux<VisitRequest> findByOrganizationId(Long organizationId) {
        return repository.findByOrganizationId(organizationId)
                .map(this::toDomain);
    }

    @Override
    public Flux<VisitRequest> findByUnitId(Long unitId) {
        return repository.findByUnitId(unitId)
                .map(this::toDomain);
    }

    @Override
    public Flux<VisitRequest> findByRequestedBy(Long userId) {
        return repository.findByRequestedBy(userId)
                .map(this::toDomain);
    }

    @Override
    public Flux<VisitRequest> findByStatus(VisitStatus status) {
        return repository.findByStatus(status.name())
                .map(this::toDomain);
    }

    @Override
    public Flux<VisitRequest> findPendingByOrganization(Long organizationId) {
        return repository.findByOrganizationIdAndStatus(organizationId, VisitStatus.PENDING.name())
                .map(this::toDomain);
    }

    @Override
    public Flux<VisitRequest> findActiveByUnit(Long unitId) {
        return repository.findByUnitIdAndValidUntilAfter(unitId, Instant.now())
                .map(this::toDomain);
    }

    @Override
    public Mono<VisitRequest> save(VisitRequest visitRequest) {
        VisitRequestEntity entity = toEntity(visitRequest);
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        entity.setUpdatedAt(Instant.now());
        return repository.save(entity)
                .map(this::toDomain);
    }

    @Override
    public Mono<Void> delete(Long id) {
        return repository.deleteById(id);
    }

    @Override
    public Mono<Long> countPendingByUnit(Long unitId) {
        return repository.countByUnitIdAndStatus(unitId, VisitStatus.PENDING.name());
    }

    private VisitRequest toDomain(VisitRequestEntity entity) {
        return VisitRequest.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .unitId(entity.getUnitId())
                .requestedBy(entity.getRequestedBy())
                .visitorName(entity.getVisitorName())
                .visitorDocument(entity.getVisitorDocument())
                .visitorPhone(entity.getVisitorPhone())
                .visitorEmail(entity.getVisitorEmail())
                .reason(entity.getReason())
                .validFrom(entity.getValidFrom())
                .validUntil(entity.getValidUntil())
                .recurrenceType(entity.getRecurrenceType() != null ? RecurrenceType.valueOf(entity.getRecurrenceType()) : null)
                .maxEntries(entity.getMaxEntries())
                .status(entity.getStatus() != null ? VisitStatus.valueOf(entity.getStatus()) : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private VisitRequestEntity toEntity(VisitRequest visitRequest) {
        return VisitRequestEntity.builder()
                .id(visitRequest.getId())
                .organizationId(visitRequest.getOrganizationId())
                .unitId(visitRequest.getUnitId())
                .requestedBy(visitRequest.getRequestedBy())
                .visitorName(visitRequest.getVisitorName())
                .visitorDocument(visitRequest.getVisitorDocument())
                .visitorPhone(visitRequest.getVisitorPhone())
                .visitorEmail(visitRequest.getVisitorEmail())
                .reason(visitRequest.getReason())
                .validFrom(visitRequest.getValidFrom())
                .validUntil(visitRequest.getValidUntil())
                .recurrenceType(visitRequest.getRecurrenceType() != null ? visitRequest.getRecurrenceType().name() : null)
                .maxEntries(visitRequest.getMaxEntries())
                .status(visitRequest.getStatus() != null ? visitRequest.getStatus().name() : null)
                .createdAt(visitRequest.getCreatedAt())
                .updatedAt(visitRequest.getUpdatedAt())
                .build();
    }
}
