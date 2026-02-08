package co.com.atlas.r2dbc.poll;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PollOptionReactiveRepository extends ReactiveCrudRepository<PollOptionEntity, Long> {

    @Query("SELECT * FROM poll_options WHERE poll_id = :pollId ORDER BY sort_order ASC")
    Flux<PollOptionEntity> findByPollId(Long pollId);

    @Modifying
    @Query("DELETE FROM poll_options WHERE poll_id = :pollId")
    Mono<Void> deleteByPollId(Long pollId);
}
