package co.com.atlas.r2dbc.access;

import co.com.atlas.model.access.AccessCode;
import co.com.atlas.model.access.AccessCodeStatus;
import co.com.atlas.model.access.CodeType;
import co.com.atlas.model.access.gateways.AccessCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Implementación del gateway AccessCodeRepository usando R2DBC.
 */
@Repository
@RequiredArgsConstructor
public class AccessCodeRepositoryAdapter implements AccessCodeRepository {

    private final AccessCodeReactiveRepository repository;

    @Override
    public Mono<AccessCode> findById(Long id) {
        return repository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public Mono<AccessCode> findByCodeHash(String codeHash) {
        return repository.findByCodeHash(codeHash)
                .map(this::toDomain);
    }

    @Override
    public Flux<AccessCode> findByVisitRequestId(Long visitRequestId) {
        return repository.findByVisitRequestId(visitRequestId)
                .map(this::toDomain);
    }

    @Override
    public Flux<AccessCode> findByStatus(AccessCodeStatus status) {
        return repository.findByStatus(status.name())
                .map(this::toDomain);
    }

    @Override
    public Mono<AccessCode> save(AccessCode accessCode) {
        AccessCodeEntity entity = toEntity(accessCode);
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
    public Mono<Boolean> existsByCodeHash(String codeHash) {
        return repository.existsByCodeHash(codeHash);
    }

    private AccessCode toDomain(AccessCodeEntity entity) {
        return AccessCode.builder()
                .id(entity.getId())
                .visitRequestId(entity.getVisitRequestId())
                .codeHash(entity.getCodeHash())
                .rawCode(null) // Never store raw code, only hash
                .codeType(entity.getCodeType() != null ? CodeType.valueOf(entity.getCodeType()) : null)
                .status(entity.getStatus() != null ? AccessCodeStatus.valueOf(entity.getStatus()) : null)
                .entriesUsed(entity.getEntriesUsed())
                .validFrom(entity.getValidFrom())
                .validUntil(entity.getValidUntil())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private AccessCodeEntity toEntity(AccessCode accessCode) {
        return AccessCodeEntity.builder()
                .id(accessCode.getId())
                .visitRequestId(accessCode.getVisitRequestId())
                .codeHash(accessCode.getCodeHash())
                .codeType(accessCode.getCodeType() != null ? accessCode.getCodeType().name() : null)
                .status(accessCode.getStatus() != null ? accessCode.getStatus().name() : null)
                .entriesUsed(accessCode.getEntriesUsed())
                .validFrom(accessCode.getValidFrom())
                .validUntil(accessCode.getValidUntil())
                .createdAt(accessCode.getCreatedAt())
                .updatedAt(accessCode.getUpdatedAt())
                .build();
    }
}
