package co.com.atlas.model.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Modelo de dominio para el estado de lectura de un mensaje.
 * Registra qué usuario leyó qué mensaje y cuándo.
 */
@Getter
@Setter
@AllArgsConstructor
@Builder(toBuilder = true)
public class MessageReadStatus {
    private Long id;
    private Long messageId;
    private Long userId;
    private Instant readAt;
}
