package co.com.atlas.r2dbc.post;

import co.com.atlas.model.common.PageResponse;
import co.com.atlas.model.common.PostPollFilter;
import co.com.atlas.model.post.Post;
import co.com.atlas.model.post.PostStatus;
import co.com.atlas.model.post.PostType;
import co.com.atlas.model.post.gateways.PostRepository;
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
public class PostRepositoryAdapter implements PostRepository {

    private final PostReactiveRepository repository;
    private final DatabaseClient databaseClient;

    @Override
    public Mono<Post> save(Post post) {
        return repository.save(toEntity(post))
                .map(this::toDomain);
    }

    @Override
    public Mono<Post> findById(Long id) {
        return repository.findById(id)
                .filter(entity -> entity.getDeletedAt() == null)
                .map(this::toDomain);
    }

    @Override
    public Flux<Post> findByOrganizationId(Long organizationId) {
        return repository.findByOrganizationId(organizationId)
                .map(this::toDomain);
    }

    @Override
    public Flux<Post> findPublishedByOrganizationId(Long organizationId) {
        return repository.findPublishedByOrganizationId(organizationId)
                .map(this::toDomain);
    }

    @Override
    public Flux<Post> findPinnedByOrganizationId(Long organizationId) {
        return repository.findPinnedByOrganizationId(organizationId)
                .map(this::toDomain);
    }

    @Override
    public Mono<Void> deleteById(Long id) {
        return repository.deleteById(id);
    }

    @Override
    public Mono<PageResponse<Post>> findByFilters(Long organizationId, PostPollFilter filter) {
        String countSql = "SELECT COUNT(*) FROM posts WHERE organization_id = :orgId AND deleted_at IS NULL"
                + buildOptionalWhere(filter);
        String dataSql = "SELECT * FROM posts WHERE organization_id = :orgId AND deleted_at IS NULL"
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

        Flux<Post> dataFlux = databaseClient.sql(dataSql)
                .bindValues(params)
                .map((row, metadata) -> Post.builder()
                        .id(row.get("id", Long.class))
                        .organizationId(row.get("organization_id", Long.class))
                        .authorId(row.get("author_id", Long.class))
                        .title(row.get("title", String.class))
                        .content(row.get("content", String.class))
                        .type(row.get("type", String.class) != null ? PostType.valueOf(row.get("type", String.class)) : null)
                        .allowComments(row.get("allow_comments", Boolean.class))
                        .isPinned(row.get("is_pinned", Boolean.class))
                        .status(row.get("status", String.class) != null ? PostStatus.valueOf(row.get("status", String.class)) : null)
                        .publishedAt(row.get("published_at", Instant.class))
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
        String sql = "SELECT status, COUNT(*) as cnt FROM posts WHERE organization_id = :orgId AND deleted_at IS NULL GROUP BY status";
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
        if (filter.getType() != null && !filter.getType().isBlank()) {
            sb.append(" AND type = :type");
        }
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
            sb.append(" AND (title ILIKE :search OR content ILIKE :search)");
        }
        return sb.toString();
    }

    private Map<String, Object> buildParams(Long organizationId, PostPollFilter filter) {
        Map<String, Object> params = new HashMap<>();
        params.put("orgId", organizationId);
        if (filter.getType() != null && !filter.getType().isBlank()) {
            params.put("type", filter.getType());
        }
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

    private Post toDomain(PostEntity entity) {
        return Post.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .authorId(entity.getAuthorId())
                .title(entity.getTitle())
                .content(entity.getContent())
                .type(entity.getType() != null ? PostType.valueOf(entity.getType()) : null)
                .allowComments(entity.getAllowComments())
                .isPinned(entity.getIsPinned())
                .status(entity.getStatus() != null ? PostStatus.valueOf(entity.getStatus()) : null)
                .publishedAt(entity.getPublishedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .deletedAt(entity.getDeletedAt())
                .build();
    }

    private PostEntity toEntity(Post post) {
        return PostEntity.builder()
                .id(post.getId())
                .organizationId(post.getOrganizationId())
                .authorId(post.getAuthorId())
                .title(post.getTitle())
                .content(post.getContent())
                .type(post.getType() != null ? post.getType().name() : null)
                .allowComments(post.getAllowComments())
                .isPinned(post.getIsPinned())
                .status(post.getStatus() != null ? post.getStatus().name() : null)
                .publishedAt(post.getPublishedAt())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .deletedAt(post.getDeletedAt())
                .build();
    }
}
