package co.com.atlas.api.config;

import co.com.atlas.api.message.websocket.ChatWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import java.util.Map;

/**
 * Configuración de WebSocket para el canal de mensajería en tiempo real.
 * Registra el endpoint /ws/chat y el adaptador necesario.
 */
@Configuration
public class WebSocketConfig {

    @Bean
    public HandlerMapping chatWebSocketMapping(ChatWebSocketHandler handler) {
        Map<String, Object> map = Map.of("/ws/chat", handler);
        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setUrlMap(map);
        mapping.setOrder(-1);
        return mapping;
    }

    @Bean
    public WebSocketHandlerAdapter webSocketHandlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}
