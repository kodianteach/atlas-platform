package co.com.atlas.r2dbc.poll;

import co.com.atlas.model.poll.PollVote;
import co.com.atlas.model.poll.gateways.PollVoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class PollVoteRepositoryAdapter implements PollVoteRepository {

    private final PollVoteReactiveRepository repository;

    @Override
    public Mono<PollVote> save(PollVote vote) {
        return repository.save(toEntity(vote))
                .map(this::toDomain);
    }

    @Override
    public Flux<PollVote> findByPollId(Long pollId) {
        return repository.findByPollId(pollId)
                .map(this::toDomain);
    }

    @Override
    public Flux<PollVote> findByOptionId(Long optionId) {
        return repository.findByOptionId(optionId)
                .map(this::toDomain);
    }

    @Override
    public Mono<Boolean> existsByPollIdAndUserId(Long pollId, Long userId) {
        return repository.countByPollIdAndUserId(pollId, userId)
                .map(count -> count > 0);
    }

    @Override
    public Mono<Long> countByOptionId(Long optionId) {
        return repository.countByOptionId(optionId);
    }

    @Override
    public Mono<Long> countByPollId(Long pollId) {
        return repository.countByPollId(pollId);
    }

    private PollVote toDomain(PollVoteEntity entity) {
        return PollVote.builder()
                .id(entity.getId())
                .pollId(entity.getPollId())
                .optionId(entity.getOptionId())
                .userId(entity.getUserId())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private PollVoteEntity toEntity(PollVote vote) {
        return PollVoteEntity.builder()
                .id(vote.getId())
                .pollId(vote.getPollId())
                .optionId(vote.getOptionId())
                .userId(vote.getUserId())
                .createdAt(vote.getCreatedAt())
                .build();
    }
}
