package co.com.atlas.r2dbc.poll;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PollVoteReactiveRepository extends ReactiveCrudRepository<PollVoteEntity, Long> {

    Flux<PollVoteEntity> findByPollId(Long pollId);

    Flux<PollVoteEntity> findByOptionId(Long optionId);

    @Query("SELECT COUNT(*) > 0 FROM poll_votes WHERE poll_id = :pollId AND user_id = :userId")
    Mono<Boolean> existsByPollIdAndUserId(Long pollId, Long userId);

    @Query("SELECT COUNT(*) FROM poll_votes WHERE option_id = :optionId")
    Mono<Long> countByOptionId(Long optionId);

    @Query("SELECT COUNT(*) FROM poll_votes WHERE poll_id = :pollId")
    Mono<Long> countByPollId(Long pollId);
}
