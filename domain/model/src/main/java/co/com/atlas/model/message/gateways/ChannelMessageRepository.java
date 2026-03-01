package co.com.atlas.model.message.gateways;

import co.com.atlas.model.message.ChannelMessage;
import co.com.atlas.model.message.MessageReadStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Gateway (interface) para persistencia de mensajes del canal de mensajería.
 */
public interface ChannelMessageRepository {

    /** Persiste un nuevo mensaje en el canal. */
    Mono<ChannelMessage> save(ChannelMessage message);

    /** Busca un mensaje por su identificador. */
    Mono<ChannelMessage> findById(Long id);

    /** Obtiene mensajes de una organización desde una fecha dada. */
    Flux<ChannelMessage> findByOrganizationId(Long organizationId, Instant since);

    /** Actualiza un mensaje existente. */
    Mono<ChannelMessage> update(ChannelMessage message);

    /** Marca un mensaje como eliminado (soft-delete). */
    Mono<Void> softDelete(Long id, Instant deletedAt);

    /** Elimina permanentemente mensajes anteriores a la fecha de corte. Retorna cantidad eliminada. */
    Mono<Long> deleteOlderThan(Instant cutoff);

    /** Persiste un estado de lectura para un mensaje y usuario. */
    Mono<MessageReadStatus> saveReadStatus(MessageReadStatus readStatus);

    /** Obtiene los estados de lectura de un mensaje. */
    Flux<MessageReadStatus> findReadStatusByMessageId(Long messageId);
}
