package co.com.atlas.api.message.scheduler;

import co.com.atlas.usecase.message.MessageRetentionUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job para la retención automática de mensajes.
 * Ejecuta limpieza diaria de mensajes con más de 30 días de antigüedad.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MessageRetentionScheduler {

    private final MessageRetentionUseCase messageRetentionUseCase;

    /**
     * Ejecuta la limpieza de mensajes antiguos diariamente a las 3:00 AM.
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanOldMessages() {
        log.info("Iniciando limpieza de mensajes antiguos (retención 30 días)...");
        messageRetentionUseCase.cleanOldMessages()
                .subscribe(
                        count -> log.info("Limpieza de mensajes completada: {} mensajes eliminados", count),
                        error -> log.error("Error en limpieza de mensajes: {}", error.getMessage())
                );
    }
}
