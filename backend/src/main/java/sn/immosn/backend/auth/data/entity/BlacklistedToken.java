package sn.immosn.backend.auth.data.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Token JWT blacklisté après déconnexion.
 * Utilise LocalDateTime (Java 8+) — @Temporal est déprécié depuis JPA 3.2.
 */
@Entity
@Table(name = "blacklisted_tokens")
@Getter
@Setter
@NoArgsConstructor
public class BlacklistedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 2000)
    private String token;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    public BlacklistedToken(String token, LocalDateTime expiryDate) {
        this.token      = token;
        this.expiryDate = expiryDate;
    }
}
