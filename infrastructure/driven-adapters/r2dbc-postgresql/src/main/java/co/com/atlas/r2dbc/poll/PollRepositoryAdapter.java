package co.com.atlas.r2dbc.poll;

import co.com.atlas.model.common.PageResponse;
import co.com.atlas.model.common.PostPollFilter;
import co.com.atlas.model.poll.Poll;
import co.com.atlas.model.poll.PollStatus;
import co.com.atlas.model.poll.gateways.PollRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class PollRepositoryAdapter implements PollRepository {

    private final PollReactiveRepository repository;
    private final DatabaseClient databaseClient;

    @Override
    public Mono<Poll> save(Poll poll) {
        return repository.save(toEntity(poll))
                .map(this::toDomain);
    }

    @Override
    public Mono<Poll> findById(Long id) {
        return repository.findById(id)
                .filter(entity -> entity.getDeletedAt() == null)
                .map(this::toDomain);
    }

    @Override
    public Flux<Poll> findByOrganizationId(Long organizationId) {
        return repository.findByOrganizationId(organizationId)
                .map(this::toDomain);
    }

    @Override
    public Flux<Poll> findActiveByOrganizationId(Long organizationId) {
        return repository.findActiveByOrganizationId(organizationId)
                .map(this::toDomain);
    }

    @Override
    public Mono<Void> deleteById(Long id) {
        return repository.deleteById(id);
    }

    @Override
    public Mono<PageResponse<Poll>> findByFilters(Long organizationId, PostPollFilter filter) {
        String countSql = "SELECT COUNT(*) FROM polls WHERE organization_id = :orgId AND deleted_at IS NULL"
                + buildOptionalWhere(filter);
        String dataSql = "SELECT * FROM polls WHERE organization_id = :orgId AND deleted_at IS NULL"
                + buildOptionalWhere(filter)
                + " ORDER BY created_at DESC LIMIT :limit OFFSET :offset";

        Map<String, Object> params = buildParams(organizationId, filter);

        Mono<Long> countMono = databaseClient.sql(countSql)
                .bindValues(params)
                .map((row, metadata) -> row.get(0, Long.class))
                .one()
                .defaultIfEmpty(0L);

        params.put("limit", filter.getSize());
        params.put("offset", filter.getPage() * filter.getSize());

        Flux<Poll> dataFlux = databaseClient.sql(dataSql)
                .bindValues(params)
                .map((row, metadata) -> Poll.builder()
                        .id(row.get("id", Long.class))
                        .organizationId(row.get("organization_id", Long.class))
                        .authorId(row.get("author_id", Long.class))
                        .title(row.get("title", String.class))
                        .description(row.get("description", String.class))
                        .allowMultiple(row.get("allow_multiple", Boolean.class))
                        .isAnonymous(row.get("is_anonymous", Boolean.class))
                        .status(row.get("status", String.class) != null ? PollStatus.valueOf(row.get("status", String.class)) : null)
                        .startsAt(row.get("starts_at", Instant.class))
                        .endsAt(row.get("ends_at", Instant.class))
                        .createdAt(row.get("created_at", Instant.class))
                        .updatedAt(row.get("updated_at", Instant.class))
                        .deletedAt(row.get("deleted_at", Instant.class))
                        .build())
                .all();

        return Mono.zip(countMono, dataFlux.collectList())
                .map(tuple -> PageResponse.of(tuple.getT2(), filter.getPage(), filter.getSize(), tuple.getT1()));
    }

    @Override
    public Mono<Map<String, Long>> countByStatusAndOrganization(Long organizationId) {
        String sql = "SELECT status, COUNT(*) as cnt FROM polls WHERE organization_id = :orgId AND deleted_at IS NULL GROUP BY status";
        return databaseClient.sql(sql)
                .bind("orgId", organizationId)
                .map((row, metadata) -> Map.entry(
                        row.get("status", String.class),
                        row.get("cnt", Long.class)))
                .all()
                .collectMap(Map.Entry::getKey, Map.Entry::getValue);
    }

    private String buildOptionalWhere(PostPollFilter filter) {
        StringBuilder sb = new StringBuilder();
        if (filter.getStatus() != null && !filter.getStatus().isBlank()) {
            sb.append(" AND status = :status");
        }
        if (filter.getDateFrom() != null) {
            sb.append(" AND created_at >= :dateFrom");
        }
        if (filter.getDateTo() != null) {
            sb.append(" AND created_at <= :dateTo");
        }
        if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
            sb.append(" AND (title ILIKE :search OR description ILIKE :search)");
        }
        return sb.toString();
    }

    private Map<String, Object> buildParams(Long organizationId, PostPollFilter filter) {
        Map<String, Object> params = new HashMap<>();
        params.put("orgId", organizationId);
        if (filter.getStatus() != null && !filter.getStatus().isBlank()) {
            params.put("status", filter.getStatus());
        }
        if (filter.getDateFrom() != null) {
            params.put("dateFrom", filter.getDateFrom());
        }
        if (filter.getDateTo() != null) {
            params.put("dateTo", filter.getDateTo());
        }
        if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
            params.put("search", "%" + filter.getSearch() + "%");
        }
        return params;
    }

    private Poll toDomain(PollEntity entity) {
        return Poll.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .authorId(entity.getAuthorId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .allowMultiple(entity.getAllowMultiple())
                .isAnonymous(entity.getIsAnonymous())
                .status(entity.getStatus() != null ? PollStatus.valueOf(entity.getStatus()) : null)
                .startsAt(entity.getStartsAt())
                .endsAt(entity.getEndsAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .deletedAt(entity.getDeletedAt())
                .build();
    }

    private PollEntity toEntity(Poll poll) {
        return PollEntity.builder()
                .id(poll.getId())
                .organizationId(poll.getOrganizationId())
                .authorId(poll.getAuthorId())
                .title(poll.getTitle())
                .description(poll.getDescription())
                .allowMultiple(poll.getAllowMultiple())
                .isAnonymous(poll.getIsAnonymous())
                .status(poll.getStatus() != null ? poll.getStatus().name() : null)
                .startsAt(poll.getStartsAt())
                .endsAt(poll.getEndsAt())
                .createdAt(poll.getCreatedAt())
                .updatedAt(poll.getUpdatedAt())
                .deletedAt(poll.getDeletedAt())
                .build();
    }
}
