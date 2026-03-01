package co.com.atlas.r2dbc.message;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

public interface ChannelMessageReactiveRepository extends ReactiveCrudRepository<ChannelMessageEntity, Long> {

    @Query("SELECT * FROM channel_messages WHERE organization_id = :orgId AND deleted_at IS NULL AND created_at > :since ORDER BY created_at ASC")
    Flux<ChannelMessageEntity> findByOrganizationIdAndCreatedAtAfter(Long orgId, Instant since);

    @Modifying
    @Query("DELETE FROM channel_messages WHERE created_at < :cutoff")
    Mono<Long> deleteByCreatedAtBefore(Instant cutoff);
}
