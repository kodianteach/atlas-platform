package co.com.atlas.model.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Modelo de dominio para mensajes del canal de mensajería privada.
 * Representa un mensaje enviado en el canal grupal de una organización.
 */
@Getter
@Setter
@AllArgsConstructor
@Builder(toBuilder = true)
public class ChannelMessage {
    private Long id;
    private Long organizationId;
    private Long senderId;
    private String senderName;
    private String senderRole;
    private String content;
    private MessageStatus status;
    private Boolean isEdited;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
}
