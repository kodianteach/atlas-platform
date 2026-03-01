package co.com.atlas.api.message;

import co.com.atlas.api.common.dto.ApiResponse;
import co.com.atlas.api.message.dto.EditMessageDto;
import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.common.NotFoundException;
import co.com.atlas.usecase.message.ChannelMessageUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Handler REST para el canal de mensajería privada.
 * Gestiona historial y operaciones CRUD de mensajes.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChannelMessageHandler {

    private final ChannelMessageUseCase channelMessageUseCase;

    /**
     * Obtiene el historial de mensajes de la organización (últimos 30 días).
     */
    public Mono<ServerResponse> getHistory(ServerRequest request) {
        Long organizationId = Long.valueOf(request.headers().firstHeader("X-Organization-Id"));
        Instant since = Instant.now().minus(30, ChronoUnit.DAYS);

        return channelMessageUseCase.getHistory(organizationId, since)
                .collectList()
                .flatMap(messages -> ServerResponse.ok()
                        .bodyValue(ApiResponse.success(messages, "Historial de mensajes obtenido exitosamente")))
                .onErrorResume(BusinessException.class, e -> {
                    log.error("Error de negocio al obtener historial: {}", e.getMessage());
                    return ServerResponse.badRequest()
                            .bodyValue(ApiResponse.error(400, e.getMessage()));
                });
    }

    /**
     * Edita un mensaje existente. Solo el remitente original puede editar.
     */
    public Mono<ServerResponse> editMessage(ServerRequest request) {
        Long messageId = Long.valueOf(request.pathVariable("id"));
        Long userId = Long.valueOf(request.headers().firstHeader("X-User-Id"));

        return request.bodyToMono(EditMessageDto.class)
                .flatMap(dto -> channelMessageUseCase.editMessage(messageId, dto.getContent(), userId))
                .flatMap(updatedMessage -> ServerResponse.ok()
                        .bodyValue(ApiResponse.success(updatedMessage, "Mensaje editado exitosamente")))
                .onErrorResume(BusinessException.class, e -> {
                    int status = e.getHttpStatus() > 0 ? e.getHttpStatus() : 400;
                    log.error("Error al editar mensaje {}: {}", messageId, e.getMessage());
                    return ServerResponse.status(status)
                            .bodyValue(ApiResponse.error(status, e.getMessage()));
                })
                .onErrorResume(NotFoundException.class, e -> {
                    log.error("Mensaje no encontrado: {}", messageId);
                    return ServerResponse.status(404)
                            .bodyValue(ApiResponse.error(404, e.getMessage()));
                });
    }

    /**
     * Elimina un mensaje (soft-delete). Solo el remitente original puede eliminar.
     */
    public Mono<ServerResponse> deleteMessage(ServerRequest request) {
        Long messageId = Long.valueOf(request.pathVariable("id"));
        Long userId = Long.valueOf(request.headers().firstHeader("X-User-Id"));

        return channelMessageUseCase.deleteMessage(messageId, userId)
                .then(ServerResponse.ok()
                        .bodyValue(ApiResponse.success(null, "Mensaje eliminado exitosamente")))
                .onErrorResume(BusinessException.class, e -> {
                    int status = e.getHttpStatus() > 0 ? e.getHttpStatus() : 400;
                    log.error("Error al eliminar mensaje {}: {}", messageId, e.getMessage());
                    return ServerResponse.status(status)
                            .bodyValue(ApiResponse.error(status, e.getMessage()));
                })
                .onErrorResume(NotFoundException.class, e -> {
                    log.error("Mensaje no encontrado: {}", messageId);
                    return ServerResponse.status(404)
                            .bodyValue(ApiResponse.error(404, e.getMessage()));
                });
    }
}
