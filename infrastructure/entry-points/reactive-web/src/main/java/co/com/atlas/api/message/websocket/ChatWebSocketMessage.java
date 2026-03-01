package co.com.atlas.api.message.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Modelo de protocolo para mensajes WebSocket del canal de mensajería.
 * Define los tipos de mensajes intercambiados entre cliente y servidor.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ChatWebSocketMessage {

    /**
     * Tipos de mensaje:
     * - Cliente → Servidor: SEND, READ, EDIT, DELETE, SYNC_REQUEST
     * - Servidor → Cliente: MESSAGE, READ_RECEIPT, EDITED, DELETED, SYNC_RESPONSE, UNREAD_COUNT
     */
    private String type;

    /**
     * Payload dinámico del mensaje (contenido varía según type).
     */
    private Object payload;
}
