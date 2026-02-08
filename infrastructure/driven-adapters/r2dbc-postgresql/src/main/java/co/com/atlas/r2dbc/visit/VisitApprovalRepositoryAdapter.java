package co.com.atlas.r2dbc.visit;

import co.com.atlas.model.visit.ApprovalAction;
import co.com.atlas.model.visit.VisitApproval;
import co.com.atlas.model.visit.gateways.VisitApprovalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Implementación del gateway VisitApprovalRepository usando R2DBC.
 */
@Repository
@RequiredArgsConstructor
public class VisitApprovalRepositoryAdapter implements VisitApprovalRepository {

    private final VisitApprovalReactiveRepository repository;

    @Override
    public Mono<VisitApproval> findById(Long id) {
        return repository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public Flux<VisitApproval> findByVisitRequestId(Long visitRequestId) {
        return repository.findByVisitRequestId(visitRequestId)
                .map(this::toDomain);
    }

    @Override
    public Flux<VisitApproval> findByApprover(Long userId) {
        return repository.findByApprovedBy(userId)
                .map(this::toDomain);
    }

    @Override
    public Mono<VisitApproval> findLatestByVisitRequest(Long visitRequestId) {
        return repository.findFirstByVisitRequestIdOrderByCreatedAtDesc(visitRequestId)
                .map(this::toDomain);
    }

    @Override
    public Mono<VisitApproval> save(VisitApproval approval) {
        VisitApprovalEntity entity = toEntity(approval);
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        return repository.save(entity)
                .map(this::toDomain);
    }

    private VisitApproval toDomain(VisitApprovalEntity entity) {
        return VisitApproval.builder()
                .id(entity.getId())
                .visitRequestId(entity.getVisitRequestId())
                .approvedBy(entity.getApprovedBy())
                .action(entity.getAction() != null ? ApprovalAction.valueOf(entity.getAction()) : null)
                .reason(entity.getReason())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private VisitApprovalEntity toEntity(VisitApproval approval) {
        return VisitApprovalEntity.builder()
                .id(approval.getId())
                .visitRequestId(approval.getVisitRequestId())
                .approvedBy(approval.getApprovedBy())
                .action(approval.getAction() != null ? approval.getAction().name() : null)
                .reason(approval.getReason())
                .createdAt(approval.getCreatedAt())
                .build();
    }
}
