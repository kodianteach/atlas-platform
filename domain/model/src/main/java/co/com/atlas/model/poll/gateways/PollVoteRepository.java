package co.com.atlas.model.poll.gateways;

import co.com.atlas.model.poll.PollVote;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Gateway para repositorio de PollVotes.
 */
public interface PollVoteRepository {
    
    Mono<PollVote> save(PollVote vote);
    
    Flux<PollVote> findByPollId(Long pollId);
    
    Flux<PollVote> findByOptionId(Long optionId);
    
    Mono<Boolean> existsByPollIdAndUserId(Long pollId, Long userId);
    
    Mono<Long> countByOptionId(Long optionId);
    
    Mono<Long> countByPollId(Long pollId);
}
