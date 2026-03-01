package co.com.atlas.usecase.message;

import co.com.atlas.model.message.gateways.ChannelMessageRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Caso de uso para la retención automática de mensajes.
 * Elimina permanentemente los mensajes con más de 30 días de antigüedad.
 */
@RequiredArgsConstructor
public class MessageRetentionUseCase {

    private final ChannelMessageRepository channelMessageRepository;

    /**
     * Elimina los mensajes con más de 30 días de antigüedad.
     *
     * @return Mono con el número de mensajes eliminados
     */
    public Mono<Long> cleanOldMessages() {
        Instant cutoff = Instant.now().minus(30, ChronoUnit.DAYS);
        return channelMessageRepository.deleteOlderThan(cutoff);
    }
}
