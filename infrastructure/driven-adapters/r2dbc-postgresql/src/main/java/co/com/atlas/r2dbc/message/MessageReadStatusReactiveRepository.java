package co.com.atlas.r2dbc.message;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface MessageReadStatusReactiveRepository extends ReactiveCrudRepository<MessageReadStatusEntity, Long> {

    Flux<MessageReadStatusEntity> findByMessageId(Long messageId);
}
