package co.com.atlas.model.poll.gateways;

import co.com.atlas.model.poll.Poll;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Gateway para repositorio de Polls.
 */
public interface PollRepository {
    
    Mono<Poll> save(Poll poll);
    
    Mono<Poll> findById(Long id);
    
    Flux<Poll> findByOrganizationId(Long organizationId);
    
    Flux<Poll> findActiveByOrganizationId(Long organizationId);
    
    Mono<Void> deleteById(Long id);
}
