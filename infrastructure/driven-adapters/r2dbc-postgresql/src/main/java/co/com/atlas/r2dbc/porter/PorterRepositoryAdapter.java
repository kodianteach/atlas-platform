package co.com.atlas.r2dbc.porter;

import co.com.atlas.model.porter.Porter;
import co.com.atlas.model.porter.PorterType;
import co.com.atlas.model.porter.gateways.PorterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Implementación del gateway PorterRepository.
 * Un portero no tiene tabla propia; se consulta via JOIN de users + user_roles_multi + role.
 */
@Repository
@RequiredArgsConstructor
public class PorterRepositoryAdapter implements PorterRepository {

    private final DatabaseClient databaseClient;

    private static final String BASE_QUERY = """
            SELECT u.id, u.names, u.email, u.status, u.created_at, u.updated_at,
                   r.code AS role_code, urm.organization_id
            FROM users u
            JOIN user_roles_multi urm ON urm.user_id = u.id
            JOIN role r ON r.id = urm.role_id
            WHERE r.code IN ('PORTERO_GENERAL', 'PORTERO_DELIVERY')
            AND u.deleted_at IS NULL
            """;

    @Override
    public Flux<Porter> findByOrganizationId(Long organizationId) {
        return databaseClient.sql(BASE_QUERY + " AND urm.organization_id = :orgId")
                .bind("orgId", organizationId)
                .map(this::mapToPorter)
                .all();
    }

    @Override
    public Mono<Porter> findByUserIdAndOrganizationId(Long userId, Long organizationId) {
        return databaseClient.sql(BASE_QUERY + " AND u.id = :userId AND urm.organization_id = :orgId")
                .bind("userId", userId)
                .bind("orgId", organizationId)
                .map(this::mapToPorter)
                .one();
    }

    private Porter mapToPorter(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata metadata) {
        return Porter.builder()
                .id(row.get("id", Long.class))
                .names(row.get("names", String.class))
                .email(row.get("email", String.class))
                .status(row.get("status", String.class))
                .porterType(PorterType.valueOf(row.get("role_code", String.class)))
                .organizationId(row.get("organization_id", Long.class))
                .createdAt(row.get("created_at", Instant.class))
                .updatedAt(row.get("updated_at", Instant.class))
                .build();
    }
}
