package co.com.atlas.r2dbc.poll;

import co.com.atlas.model.poll.PollOption;
import co.com.atlas.model.poll.gateways.PollOptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class PollOptionRepositoryAdapter implements PollOptionRepository {

    private final PollOptionReactiveRepository repository;

    @Override
    public Mono<PollOption> save(PollOption option) {
        return repository.save(toEntity(option))
                .map(this::toDomain);
    }

    @Override
    public Flux<PollOption> saveAll(Flux<PollOption> options) {
        return repository.saveAll(options.map(this::toEntity))
                .map(this::toDomain);
    }

    @Override
    public Mono<PollOption> findById(Long id) {
        return repository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public Flux<PollOption> findByPollId(Long pollId) {
        return repository.findByPollId(pollId)
                .map(this::toDomain);
    }

    @Override
    public Mono<Void> deleteByPollId(Long pollId) {
        return repository.deleteByPollId(pollId);
    }

    private PollOption toDomain(PollOptionEntity entity) {
        return PollOption.builder()
                .id(entity.getId())
                .pollId(entity.getPollId())
                .optionText(entity.getOptionText())
                .sortOrder(entity.getSortOrder())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private PollOptionEntity toEntity(PollOption option) {
        return PollOptionEntity.builder()
                .id(option.getId())
                .pollId(option.getPollId())
                .optionText(option.getOptionText())
                .sortOrder(option.getSortOrder())
                .createdAt(option.getCreatedAt())
                .build();
    }
}
