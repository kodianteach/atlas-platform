package co.com.atlas.api.message.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestiona las sesiones WebSocket activas, agrupadas por organización.
 * Soporta broadcast de mensajes a todos los miembros de una organización.
 */
@Component
@Slf4j
public class ChatSessionManager {

    private final Map<Long, Set<WebSocketSession>> sessionsByOrg = new ConcurrentHashMap<>();

    /**
     * Registra una sesión WebSocket para una organización.
     */
    public void addSession(Long orgId, WebSocketSession session) {
        sessionsByOrg
                .computeIfAbsent(orgId, k -> ConcurrentHashMap.newKeySet())
                .add(session);
        log.info("Session added for org {}: {} (total: {})", orgId, session.getId(),
                sessionsByOrg.getOrDefault(orgId, Collections.emptySet()).size());
    }

    /**
     * Elimina una sesión WebSocket de una organización.
     */
    public void removeSession(Long orgId, WebSocketSession session) {
        Set<WebSocketSession> sessions = sessionsByOrg.get(orgId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                sessionsByOrg.remove(orgId);
            }
            log.info("Session removed for org {}: {} (remaining: {})", orgId, session.getId(),
                    sessions.size());
        }
    }

    /**
     * Obtiene todas las sesiones activas de una organización.
     */
    public Set<WebSocketSession> getSessionsByOrg(Long orgId) {
        return sessionsByOrg.getOrDefault(orgId, Collections.emptySet());
    }
}
