package sn.immosn.backend.websocket.security;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import sn.immosn.backend.auth.data.entity.User;
import sn.immosn.backend.auth.data.jwt.JwtTokenProvider;
import sn.immosn.backend.auth.service.AuthUserDetailService;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Intercepteur STOMP responsable de l'authentification et de l'autorisation WebSocket.
 *
 * CONNECT  : valide le JWT (header Authorization ou session attribute), injecte le Principal.
 * SUBSCRIBE: applique un whitelist strict des destinations autorisées.
 *   - /user/queue/notifications     → tous les utilisateurs authentifiés
 *   - /topic/admin.notifications    → ADMIN / SUPER_ADMIN uniquement
 */
@RequiredArgsConstructor
public class WebSocketChannelInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketChannelInterceptor.class);

    private final JwtTokenProvider    jwtTokenProvider;
    private final AuthUserDetailService authUserDetailService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
            MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            handleConnect(accessor);
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            handleSubscribe(accessor);
        }

        return message;
    }

    // CONNECT : extrait + valide le JWT, injecte WebSocketPrincipal

    private void handleConnect(StompHeaderAccessor accessor) {
        String token = resolveToken(accessor);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            log.warn("[WS] Connexion refusée — token absent ou invalide");
            throw new MessagingException("Connexion WebSocket refusée : token JWT absent ou invalide");
        }

        String email = jwtTokenProvider.getUserNameFromToken(token);
        User user = (User) authUserDetailService.loadUserByUsername(email);
        var authorities = user.getAuthorities().stream()
            .map(a -> a.getAuthority())
            .collect(Collectors.toSet());

        accessor.setUser(new WebSocketPrincipal(email, user.getId(), authorities));
        log.info("[WS] Connexion établie : userId={} email={}", user.getId(), email);
    }

    // SUBSCRIBE : whitelist stricte des destinations

    private void handleSubscribe(StompHeaderAccessor accessor) {
        WebSocketPrincipal principal = (WebSocketPrincipal) accessor.getUser();
        if (principal == null) {
            throw new MessagingException("Abonnement refusé — non authentifié");
        }

        String dest = accessor.getDestination();
        if (dest == null) {
            throw new MessagingException("Abonnement refusé — destination nulle");
        }

        if ("/user/queue/notifications".equals(dest)) {
            // Autorisé pour tout utilisateur authentifié
            log.debug("[WS] Abonnement autorisé : {} → {}", principal.getName(), dest);

        } else if (dest.startsWith("/topic/admin")) {
            if (!principal.isAdmin()) {
                log.warn("[WS] Accès refusé admin topic : user={} dest={}", principal.getName(), dest);
                throw new MessagingException("Abonnement refusé — rôle ADMIN requis pour " + dest);
            }
            log.debug("[WS] Abonnement admin autorisé : {} → {}", principal.getName(), dest);

        } else {
            log.warn("[WS] Destination non whitelistée : user={} dest={}", principal.getName(), dest);
            throw new MessagingException("Destination WebSocket non autorisée : " + dest);
        }
    }

    // Résolution du token : header STOMP Authorization > session attribute jwt

    private String resolveToken(StompHeaderAccessor accessor) {
        // 1. Header STOMP Authorization: Bearer <token>  (méthode recommandée)
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        // 2. Session attribute "jwt" positionné par JwtHandshakeInterceptor (URL param)
        Map<String, Object> attrs = accessor.getSessionAttributes();
        if (attrs != null) {
            Object jwt = attrs.get("jwt");
            if (jwt instanceof String s && !s.isBlank()) {
                return s;
            }
        }
        return null;
    }
}
