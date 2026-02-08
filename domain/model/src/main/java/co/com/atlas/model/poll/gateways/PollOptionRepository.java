package co.com.atlas.model.poll.gateways;

import co.com.atlas.model.poll.PollOption;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Gateway para repositorio de PollOptions.
 */
public interface PollOptionRepository {
    
    Mono<PollOption> save(PollOption option);
    
    Flux<PollOption> saveAll(Flux<PollOption> options);
    
    Mono<PollOption> findById(Long id);
    
    Flux<PollOption> findByPollId(Long pollId);
    
    Mono<Void> deleteByPollId(Long pollId);
}
