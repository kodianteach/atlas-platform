package co.com.atlas.r2dbc.poll;

import co.com.atlas.model.poll.Poll;
import co.com.atlas.model.poll.PollStatus;
import co.com.atlas.model.poll.gateways.PollRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class PollRepositoryAdapter implements PollRepository {

    private final PollReactiveRepository repository;

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
