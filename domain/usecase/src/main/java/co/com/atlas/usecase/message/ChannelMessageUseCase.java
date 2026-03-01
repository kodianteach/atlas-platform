package co.com.atlas.usecase.message;

import co.com.atlas.model.common.BusinessException;
import co.com.atlas.model.common.NotFoundException;
import co.com.atlas.model.message.ChannelMessage;
import co.com.atlas.model.message.MessageReadStatus;
import co.com.atlas.model.message.MessageStatus;
import co.com.atlas.model.message.gateways.ChannelMessageRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Caso de uso para gestión de mensajes del canal de mensajería privada.
 */
@RequiredArgsConstructor
public class ChannelMessageUseCase {

    private final ChannelMessageRepository channelMessageRepository;

    /**
     * Envía un nuevo mensaje en el canal de mensajería.
     */
    public Mono<ChannelMessage> sendMessage(ChannelMessage message) {
        if (message.getContent() == null || message.getContent().trim().isEmpty()) {
            return Mono.error(new BusinessException("El contenido del mensaje no puede estar vacío"));
        }

        ChannelMessage toSave = message.toBuilder()
                .status(MessageStatus.SENT)
                .isEdited(false)
                .createdAt(Instant.now())
                .build();

        return channelMessageRepository.save(toSave);
    }

    /**
     * Obtiene el historial de mensajes de una organización (últimos 30 días).
     */
    public Flux<ChannelMessage> getHistory(Long organizationId, Instant since) {
        Instant cutoff = Instant.now().minus(30, ChronoUnit.DAYS);
        Instant effectiveSince = (since != null && since.isAfter(cutoff)) ? since : cutoff;
        return channelMessageRepository.findByOrganizationId(organizationId, effectiveSince);
    }

    /**
     * Edita un mensaje existente. Solo el remitente original puede editar.
     */
    public Mono<ChannelMessage> editMessage(Long messageId, String newContent, Long userId) {
        if (newContent == null || newContent.trim().isEmpty()) {
            return Mono.error(new BusinessException("El contenido del mensaje no puede estar vacío"));
        }

        return channelMessageRepository.findById(messageId)
                .switchIfEmpty(Mono.error(new NotFoundException("Mensaje", messageId)))
                .flatMap(existing -> {
                    if (!existing.getSenderId().equals(userId)) {
                        return Mono.error(new BusinessException(
                                "Solo puedes editar tus propios mensajes", "FORBIDDEN", 403));
                    }
                    if (existing.getDeletedAt() != null) {
                        return Mono.error(new BusinessException("No se puede editar un mensaje eliminado"));
                    }

                    ChannelMessage updated = existing.toBuilder()
                            .content(newContent.trim())
                            .isEdited(true)
                            .updatedAt(Instant.now())
                            .build();

                    return channelMessageRepository.update(updated);
                });
    }

    /**
     * Elimina un mensaje (soft-delete). Solo el remitente original puede eliminar.
     */
    public Mono<Void> deleteMessage(Long messageId, Long userId) {
        return channelMessageRepository.findById(messageId)
                .switchIfEmpty(Mono.error(new NotFoundException("Mensaje", messageId)))
                .flatMap(existing -> {
                    if (!existing.getSenderId().equals(userId)) {
                        return Mono.error(new BusinessException(
                                "Solo puedes eliminar tus propios mensajes", "FORBIDDEN", 403));
                    }

                    return channelMessageRepository.softDelete(messageId, Instant.now());
                });
    }

    /**
     * Marca un mensaje como leído por un usuario.
     */
    public Mono<MessageReadStatus> markAsRead(Long messageId, Long userId) {
        MessageReadStatus readStatus = MessageReadStatus.builder()
                .messageId(messageId)
                .userId(userId)
                .readAt(Instant.now())
                .build();

        return channelMessageRepository.saveReadStatus(readStatus);
    }

    /**
     * Obtiene el estado de lectura de un mensaje.
     */
    public Flux<MessageReadStatus> getReadStatus(Long messageId) {
        return channelMessageRepository.findReadStatusByMessageId(messageId);
    }
}
