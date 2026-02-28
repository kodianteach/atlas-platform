package co.com.atlas.model.poll.gateways;

import co.com.atlas.model.common.PageResponse;
import co.com.atlas.model.common.PostPollFilter;
import co.com.atlas.model.poll.Poll;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Gateway para repositorio de Polls.
 */
public interface PollRepository {
    
    Mono<Poll> save(Poll poll);
    
    Mono<Poll> findById(Long id);
    
    Flux<Poll> findByOrganizationId(Long organizationId);
    
    Flux<Poll> findActiveByOrganizationId(Long organizationId);
    
    Mono<Void> deleteById(Long id);

    Mono<PageResponse<Poll>> findByFilters(Long organizationId, PostPollFilter filter);

    Mono<Map<String, Long>> countByStatusAndOrganization(Long organizationId);
}
