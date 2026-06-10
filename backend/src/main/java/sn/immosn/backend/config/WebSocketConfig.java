package sn.immosn.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import sn.immosn.backend.auth.data.jwt.JwtTokenProvider;
import sn.immosn.backend.auth.service.AuthUserDetailService;
import sn.immosn.backend.websocket.security.JwtHandshakeInterceptor;
import sn.immosn.backend.websocket.security.WebSocketChannelInterceptor;

import java.util.List;

/**
 * Configuration WebSocket / STOMP.
 *
 * Endpoint  : /ws  (WebSocket natif — pas de SockJS pour compatibilité Spring 7.x)
 * Broker    : SimpleBroker in-memory sur /topic et /queue
 * App prefix: /app  (envoi client → serveur)
 * User prefix: /user (convertAndSendToUser)
 *
 * Sécurité :
 *   - JwtHandshakeInterceptor  : stocke le token URL en session
 *   - WebSocketChannelInterceptor : valide le JWT au CONNECT, whitelist les souscriptions
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtTokenProvider      jwtTokenProvider;
    private final AuthUserDetailService authUserDetailService;

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:80}")
    private List<String> allowedOrigins;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
            .setAllowedOrigins(allowedOrigins.toArray(new String[0]))
            .addInterceptors(new JwtHandshakeInterceptor(jwtTokenProvider));
        // Note : .withSockJS() retiré (supprimé dans Spring Framework 7.x)
        // Le client frontend utilise directement WebSocket natif via @stomp/stompjs
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(
            new WebSocketChannelInterceptor(jwtTokenProvider, authUserDetailService)
        );
    }
}
