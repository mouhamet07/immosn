package sn.immosn.backend.websocket.security;

import java.security.Principal;
import java.util.Set;

/**
 * Principal WebSocket portant l'identité complète de l'utilisateur connecté.
 * Injecté par WebSocketChannelInterceptor lors du frame STOMP CONNECT.
 * getName() est l'email — correspond à l'argument de convertAndSendToUser().
 */
public class WebSocketPrincipal implements Principal {

    private final String email;
    private final Long   userId;
    private final Set<String> authorities; // e.g. "ROLE_CLIENT", "ROLE_ADMIN"

    public WebSocketPrincipal(String email, Long userId, Set<String> authorities) {
        this.email       = email;
        this.userId      = userId;
        this.authorities = authorities;
    }

    @Override
    public String getName() {
        return email;
    }

    public Long getUserId() {
        return userId;
    }

    public boolean isAdmin() {
        return authorities.contains("ROLE_ADMIN") || authorities.contains("ROLE_SUPER_ADMIN");
    }
}
