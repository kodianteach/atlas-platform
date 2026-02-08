package co.com.atlas.r2dbc.invitation;

import co.com.atlas.model.invitation.Invitation;
import co.com.atlas.model.invitation.InvitationStatus;
import co.com.atlas.model.invitation.InvitationType;
import co.com.atlas.model.invitation.gateways.InvitationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Implementación del gateway InvitationRepository usando R2DBC.
 */
@Repository
@RequiredArgsConstructor
public class InvitationRepositoryAdapter implements InvitationRepository {

    private final InvitationReactiveRepository repository;

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
                .phoneNumber(entity.getPhoneNumber())
                .invitationToken(entity.getInvitationToken())
                .type(entity.getType() != null ? InvitationType.valueOf(entity.getType()) : null)
                .roleId(entity.getRoleId())
                .status(entity.getStatus() != null ? InvitationStatus.valueOf(entity.getStatus()) : null)
                .invitedBy(entity.getInvitedBy())
                .expiresAt(entity.getExpiresAt())
                .acceptedAt(entity.getAcceptedAt())
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
                .phoneNumber(invitation.getPhoneNumber())
                .invitationToken(invitation.getInvitationToken())
                .type(invitation.getType() != null ? invitation.getType().name() : null)
                .roleId(invitation.getRoleId())
                .status(invitation.getStatus() != null ? invitation.getStatus().name() : null)
                .invitedBy(invitation.getInvitedBy())
                .expiresAt(invitation.getExpiresAt())
                .acceptedAt(invitation.getAcceptedAt())
                .createdAt(invitation.getCreatedAt())
                .updatedAt(invitation.getUpdatedAt())
                .build();
    }
}
