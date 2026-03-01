package co.com.atlas.api.message.websocket;

import co.com.atlas.model.auth.gateways.JwtTokenGateway;
import co.com.atlas.model.message.ChannelMessage;
import co.com.atlas.model.message.MessageStatus;
import co.com.atlas.usecase.message.ChannelMessageUseCase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * WebSocket handler para el canal de mensajería en tiempo real.
 * Procesa mensajes entrantes y los difunde a todos los participantes de la organización.
 */
@Component
@Slf4j
public class ChatWebSocketHandler implements WebSocketHandler {

    private final ChatSessionManager sessionManager;
    private final ChannelMessageUseCase channelMessageUseCase;
    private final JwtTokenGateway jwtTokenGateway;
    private final ObjectMapper objectMapper;

    public ChatWebSocketHandler(ChatSessionManager sessionManager,
                                 ChannelMessageUseCase channelMessageUseCase,
                                 JwtTokenGateway jwtTokenGateway) {
        this.sessionManager = sessionManager;
        this.channelMessageUseCase = channelMessageUseCase;
        this.jwtTokenGateway = jwtTokenGateway;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        // Extraer token de query param
        String query = session.getHandshakeInfo().getUri().getQuery();
        String token = extractTokenFromQuery(query);

        if (token == null || token.isEmpty()) {
            log.warn("WebSocket connection rejected: no token");
            return session.close();
        }

        // Validar token y extraer datos del usuario
        return jwtTokenGateway.validateToken(token)
                .flatMap(valid -> {
                    if (!valid) {
                        log.warn("WebSocket connection rejected: invalid token");
                        return Mono.<Void>from(session.close());
                    }
                    return Mono.zip(
                            jwtTokenGateway.extractUserId(token),
                            jwtTokenGateway.extractRole(token)
                    ).flatMap(tuple -> {
                        String userIdStr = tuple.getT1();
                        String role = tuple.getT2();

                    // Extraer organizationId y senderName del query params
                    Long userId = Long.parseLong(userIdStr);
                    Long orgId = extractLongParam(query, "orgId");
                    String senderName = extractParam(query, "name");

                    if (orgId == null) {
                        log.warn("WebSocket connection rejected: no orgId");
                        return session.close();
                    }

                    // Registrar sesión
                    sessionManager.addSession(orgId, session);

                    // Procesar mensajes entrantes
                    Flux<WebSocketMessage> incoming = session.receive()
                            .flatMap(wsMessage -> handleIncoming(wsMessage, session, orgId, userId, senderName, role))
                            .doOnError(err -> log.error("WebSocket error for user {}: {}", userId, err.getMessage()))
                            .doFinally(sig -> sessionManager.removeSession(orgId, session));

                    return session.send(incoming);
                    });  // end flatMap(tuple)
                })  // end flatMap(valid)
                .onErrorResume(e -> {
                    log.error("WebSocket error: {}", e.getMessage());
                    return session.close();
                });
    }

    private Mono<WebSocketMessage> handleIncoming(WebSocketMessage wsMessage,
                                                   WebSocketSession session,
                                                   Long orgId,
                                                   Long userId,
                                                   String senderName,
                                                   String role) {
        try {
            ChatWebSocketMessage chatMsg = objectMapper.readValue(
                    wsMessage.getPayloadAsText(), ChatWebSocketMessage.class);

            return switch (chatMsg.getType()) {
                case "SEND" -> handleSend(chatMsg, session, orgId, userId, senderName, role);
                case "READ" -> handleRead(chatMsg, session, orgId, userId);
                case "EDIT" -> handleEdit(chatMsg, session, orgId, userId);
                case "DELETE" -> handleDelete(chatMsg, session, orgId, userId);
                case "SYNC_REQUEST" -> handleSyncRequest(chatMsg, session, orgId);
                default -> {
                    log.warn("Unknown message type: {}", chatMsg.getType());
                    yield Mono.empty();
                }
            };
        } catch (JsonProcessingException e) {
            log.error("Failed to parse WebSocket message: {}", e.getMessage());
            return Mono.empty();
        }
    }

    private Mono<WebSocketMessage> handleSend(ChatWebSocketMessage chatMsg,
                                               WebSocketSession session,
                                               Long orgId,
                                               Long userId,
                                               String senderName,
                                               String role) {
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) chatMsg.getPayload();
        String content = (String) payload.get("content");

        ChannelMessage message = ChannelMessage.builder()
                .organizationId(orgId)
                .senderId(userId)
                .senderName(senderName != null ? senderName : "Usuario")
                .senderRole(role)
                .content(content)
                .status(MessageStatus.SENT)
                .isEdited(false)
                .createdAt(Instant.now())
                .build();

        return channelMessageUseCase.sendMessage(message)
                .flatMap(saved -> {
                    ChatWebSocketMessage response = ChatWebSocketMessage.builder()
                            .type("MESSAGE")
                            .payload(saved)
                            .build();
                    broadcastToOrg(orgId, response, session);
                    return Mono.justOrEmpty(createWsMessage(session, response));
                });
    }

    private Mono<WebSocketMessage> handleRead(ChatWebSocketMessage chatMsg,
                                               WebSocketSession session,
                                               Long orgId,
                                               Long userId) {
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) chatMsg.getPayload();
        Long messageId = ((Number) payload.get("messageId")).longValue();

        return channelMessageUseCase.markAsRead(messageId, userId)
                .flatMap(readStatus -> {
                    Map<String, Object> receiptPayload = new LinkedHashMap<>();
                    receiptPayload.put("messageId", messageId);
                    receiptPayload.put("userId", userId);
                    ChatWebSocketMessage response = ChatWebSocketMessage.builder()
                            .type("READ_RECEIPT")
                            .payload(receiptPayload)
                            .build();
                    broadcastToOrg(orgId, response, null);
                    return Mono.empty();
                });
    }

    private Mono<WebSocketMessage> handleEdit(ChatWebSocketMessage chatMsg,
                                               WebSocketSession session,
                                               Long orgId,
                                               Long userId) {
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) chatMsg.getPayload();
        Long messageId = ((Number) payload.get("messageId")).longValue();
        String newContent = (String) payload.get("content");

        return channelMessageUseCase.editMessage(messageId, newContent, userId)
                .flatMap(updated -> {
                    ChatWebSocketMessage response = ChatWebSocketMessage.builder()
                            .type("EDITED")
                            .payload(updated)
                            .build();
                    broadcastToOrg(orgId, response, null);
                    return Mono.justOrEmpty(createWsMessage(session, response));
                });
    }

    private Mono<WebSocketMessage> handleDelete(ChatWebSocketMessage chatMsg,
                                                 WebSocketSession session,
                                                 Long orgId,
                                                 Long userId) {
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) chatMsg.getPayload();
        Long messageId = ((Number) payload.get("messageId")).longValue();

        return channelMessageUseCase.deleteMessage(messageId, userId)
                .then(Mono.fromCallable(() -> {
                    Map<String, Object> deletedPayload = new LinkedHashMap<>();
                    deletedPayload.put("messageId", messageId);
                    ChatWebSocketMessage response = ChatWebSocketMessage.builder()
                            .type("DELETED")
                            .payload(deletedPayload)
                            .build();
                    broadcastToOrg(orgId, response, null);
                    return createWsMessage(session, response);
                }));
    }

    private Mono<WebSocketMessage> handleSyncRequest(ChatWebSocketMessage chatMsg,
                                                      WebSocketSession session,
                                                      Long orgId) {
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) chatMsg.getPayload();
        String lastTimestamp = (String) payload.get("lastTimestamp");
        Instant since = lastTimestamp != null ? Instant.parse(lastTimestamp) : Instant.now().minusSeconds(86400);

        return channelMessageUseCase.getHistory(orgId, since)
                .collectList()
                .map(messages -> {
                    ChatWebSocketMessage response = ChatWebSocketMessage.builder()
                            .type("SYNC_RESPONSE")
                            .payload(messages)
                            .build();
                    return createWsMessage(session, response);
                });
    }

    /**
     * Envía un mensaje a todas las sesiones de una organización, excluyendo opcionalmente al remitente.
     */
    private void broadcastToOrg(Long orgId, ChatWebSocketMessage message, WebSocketSession excludeSession) {
        Set<WebSocketSession> sessions = sessionManager.getSessionsByOrg(orgId);
        String json = toJson(message);

        for (WebSocketSession ws : sessions) {
            if (ws.isOpen() && (excludeSession == null || !ws.getId().equals(excludeSession.getId()))) {
                ws.send(Mono.just(ws.textMessage(json))).subscribe();
            }
        }
    }

    private WebSocketMessage createWsMessage(WebSocketSession session, ChatWebSocketMessage message) {
        return session.textMessage(toJson(message));
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize message: {}", e.getMessage());
            return "{}";
        }
    }

    private String extractTokenFromQuery(String query) {
        return extractParam(query, "token");
    }

    private String extractParam(String query, String paramName) {
        if (query == null) return null;
        for (String param : query.split("&")) {
            String[] kv = param.split("=", 2);
            if (kv.length == 2 && kv[0].equals(paramName)) {
                return java.net.URLDecoder.decode(kv[1], java.nio.charset.StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private Long extractLongParam(String query, String paramName) {
        String value = extractParam(query, paramName);
        if (value != null) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
