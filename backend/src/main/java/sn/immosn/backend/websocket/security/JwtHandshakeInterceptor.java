package sn.immosn.backend.websocket.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import sn.immosn.backend.auth.data.jwt.JwtTokenProvider;

import java.util.Map;

/**
 * Intercepteur HTTP de handshake WebSocket.
 *
 * Lit le token JWT depuis le query param ?token=<jwt> et le stocke dans
 * les attributs de session pour que WebSocketChannelInterceptor puisse y accéder
 * lors du frame STOMP CONNECT.
 *
 * Défense en profondeur : la validation principale est dans WebSocketChannelInterceptor.
 * Ici on stocke uniquement — on ne rejette pas une connexion sans token car
 * le client peut aussi envoyer le token dans les headers STOMP CONNECT.
 */
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(JwtHandshakeInterceptor.class);

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpRequest = servletRequest.getServletRequest();
            String token = httpRequest.getParameter("token");
            if (token != null && !token.isBlank() && jwtTokenProvider.validateToken(token)) {
                attributes.put("jwt", token);
                log.debug("[WS Handshake] Token URL param stocké dans la session");
            }
        }
        // Toujours accepter — la sécurité définitive est dans WebSocketChannelInterceptor (CONNECT)
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }
}
