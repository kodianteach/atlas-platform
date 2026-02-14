package co.com.atlas.r2dbc.invitation;

import co.com.atlas.model.invitation.Invitation;
import co.com.atlas.model.invitation.InvitationStatus;
import co.com.atlas.model.invitation.InvitationType;
import co.com.atlas.model.invitation.gateways.InvitationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

/**
 * Implementación del gateway InvitationRepository usando R2DBC.
 */
@Repository
@RequiredArgsConstructor
public class InvitationRepositoryAdapter implements InvitationRepository {

    private final InvitationReactiveRepository repository;
    private final DatabaseClient databaseClient;

    @Override
    public Mono<Invitation> findById(Long id) {
        return repository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public Mono<Invitation> findByToken(String token) {
        return repository.findByInvitationToken(token)
                .map(this::toDomain);
    }

    @Override
    public Flux<Invitation> findByOrganizationId(Long organizationId) {
        return repository.findByOrganizationId(organizationId)
                .map(this::toDomain);
    }

    @Override
    public Flux<Invitation> findByUnitId(Long unitId) {
        return repository.findByUnitId(unitId)
                .map(this::toDomain);
    }

    @Override
    public Flux<Invitation> findByEmail(String email) {
        return repository.findByEmail(email)
                .map(this::toDomain);
    }

    @Override
    public Flux<Invitation> findPendingByEmail(String email) {
        return repository.findByEmailAndStatus(email, InvitationStatus.PENDING.name())
                .map(this::toDomain);
    }

    @Override
    public Mono<Invitation> save(Invitation invitation) {
        InvitationEntity entity = toEntity(invitation);
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
    public Mono<Boolean> existsPendingByEmailAndOrganizationId(String email, Long organizationId) {
        return repository.existsByEmailAndOrganizationIdAndStatus(email, organizationId, InvitationStatus.PENDING.name());
    }

    private Invitation toDomain(InvitationEntity entity) {
        return Invitation.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .unitId(entity.getUnitId())
                .email(entity.getEmail())
                .invitationToken(entity.getInvitationToken())
                .type(entity.getType() != null ? InvitationType.valueOf(entity.getType()) : null)
                .roleId(entity.getRoleId())
                .initialPermissions(entity.getInitialPermissions())
                .status(entity.getStatus() != null ? InvitationStatus.valueOf(entity.getStatus()) : null)
                .invitedBy(entity.getInvitedBy())
                .expiresAt(entity.getExpiresAt())
                .acceptedAt(entity.getAcceptedAt())
                .invitationSentAt(entity.getInvitationSentAt())
                .invitationMailStatus(entity.getInvitationMailStatus())
                .retryCount(entity.getRetryCount())
                .lastRetryAt(entity.getLastRetryAt())
                .metadata(entity.getMetadata())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private InvitationEntity toEntity(Invitation invitation) {
        return InvitationEntity.builder()
                .id(invitation.getId())
                .organizationId(invitation.getOrganizationId())
                .unitId(invitation.getUnitId())
                .email(invitation.getEmail())
                .invitationToken(invitation.getInvitationToken())
                .type(invitation.getType() != null ? invitation.getType().name() : null)
                .roleId(invitation.getRoleId())
                .initialPermissions(invitation.getInitialPermissions())
                .status(invitation.getStatus() != null ? invitation.getStatus().name() : null)
                .invitedBy(invitation.getInvitedBy())
                .expiresAt(invitation.getExpiresAt())
                .acceptedAt(invitation.getAcceptedAt())
                .invitationSentAt(invitation.getInvitationSentAt())
                .invitationMailStatus(invitation.getInvitationMailStatus())
                .retryCount(invitation.getRetryCount())
                .lastRetryAt(invitation.getLastRetryAt())
                .metadata(invitation.getMetadata())
                .createdAt(invitation.getCreatedAt())
                .updatedAt(invitation.getUpdatedAt())
                .build();
    }
    
    @Override
    public Flux<Invitation> findByIdIn(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Flux.empty();
        }
        return repository.findAllById(ids)
                .map(this::toDomain);
    }
    
    @Override
    public Mono<Invitation> updateMailStatus(Long id, String mailStatus, Instant sentAt) {
        Instant now = Instant.now();
        var spec = databaseClient.sql("""
            UPDATE invitations 
            SET invitation_status = :status, 
                invitation_sent_at = :sentAt,
                updated_at = :now 
            WHERE id = :id
            """)
                .bind("status", mailStatus)
                .bind("now", now)
                .bind("id", id);
        
        if (sentAt != null) {
            spec = spec.bind("sentAt", sentAt);
        } else {
            spec = spec.bindNull("sentAt", Instant.class);
        }
        
        return spec.then()
                .then(findById(id));
    }
    
    @Override
    public Mono<Invitation> incrementRetryCount(Long id) {
        Instant now = Instant.now();
        return databaseClient.sql("""
            UPDATE invitations 
            SET retry_count = COALESCE(retry_count, 0) + 1, 
                last_retry_at = :now,
                updated_at = :now 
            WHERE id = :id
            """)
                .bind("now", now)
                .bind("id", id)
                .then()
                .then(findById(id));
    }
    
    @Override
    public Flux<Invitation> findPendingMailByOrganizationId(Long organizationId) {
        return databaseClient.sql("""
            SELECT * FROM invitations 
            WHERE organization_id = :organizationId 
            AND status = 'PENDING'
            AND (invitation_status IS NULL OR invitation_status = 'PENDING' OR invitation_status = 'FAILED')
            ORDER BY created_at ASC
            """)
                .bind("organizationId", organizationId)
                .map((row, metadata) -> InvitationEntity.builder()
                        .id(row.get("id", Long.class))
                        .organizationId(row.get("organization_id", Long.class))
                        .unitId(row.get("unit_id", Long.class))
                        .email(row.get("email", String.class))
                        .invitationToken(row.get("invitation_token", String.class))
                        .type(row.get("type", String.class))
                        .roleId(row.get("role_id", Long.class))
                        .initialPermissions(row.get("initial_permissions", String.class))
                        .status(row.get("status", String.class))
                        .invitedBy(row.get("invited_by_user_id", Long.class))
                        .expiresAt(row.get("expires_at", Instant.class))
                        .acceptedAt(row.get("accepted_at", Instant.class))
                        .invitationSentAt(row.get("invitation_sent_at", Instant.class))
                        .invitationMailStatus(row.get("invitation_status", String.class))
                        .retryCount(row.get("retry_count", Integer.class))
                        .lastRetryAt(row.get("last_retry_at", Instant.class))
                        .metadata(row.get("metadata", String.class))
                        .createdAt(row.get("created_at", Instant.class))
                        .updatedAt(row.get("updated_at", Instant.class))
                        .build())
                .all()
                .map(this::toDomain);
    }
    
    @Override
    public Mono<Boolean> existsPendingByEmailAndUnitId(String email, Long unitId) {
        return repository.existsByEmailAndUnitIdAndStatus(email, unitId, InvitationStatus.PENDING.name());
    }
    
    @Override
    public Flux<Invitation> findByOrganizationIdAndUnitId(Long organizationId, Long unitId) {
        return databaseClient.sql("""
            SELECT * FROM invitations 
            WHERE organization_id = :organizationId 
            AND unit_id = :unitId
            ORDER BY created_at DESC
            """)
                .bind("organizationId", organizationId)
                .bind("unitId", unitId)
                .map((row, metadata) -> InvitationEntity.builder()
                        .id(row.get("id", Long.class))
                        .organizationId(row.get("organization_id", Long.class))
                        .unitId(row.get("unit_id", Long.class))
                        .email(row.get("email", String.class))
                        .invitationToken(row.get("invitation_token", String.class))
                        .type(row.get("type", String.class))
                        .roleId(row.get("role_id", Long.class))
                        .initialPermissions(row.get("initial_permissions", String.class))
                        .status(row.get("status", String.class))
                        .invitedBy(row.get("invited_by_user_id", Long.class))
                        .expiresAt(row.get("expires_at", Instant.class))
                        .acceptedAt(row.get("accepted_at", Instant.class))
                        .invitationSentAt(row.get("invitation_sent_at", Instant.class))
                        .invitationMailStatus(row.get("invitation_status", String.class))
                        .retryCount(row.get("retry_count", Integer.class))
                        .lastRetryAt(row.get("last_retry_at", Instant.class))
                        .metadata(row.get("metadata", String.class))
                        .createdAt(row.get("created_at", Instant.class))
                        .updatedAt(row.get("updated_at", Instant.class))
                        .build())
                .all()
                .map(this::toDomain);
    }
}
