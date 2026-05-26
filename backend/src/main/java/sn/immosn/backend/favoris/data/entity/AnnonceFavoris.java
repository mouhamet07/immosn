package sn.immosn.backend.favoris.data.entity;

import jakarta.persistence.*;
import lombok.*;
import sn.immosn.backend.annonce.data.entity.Annonce;
import sn.immosn.backend.auth.data.entity.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "annonce_favoris")
@IdClass(AnnonceFavorisId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnonceFavoris {

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private User client;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "annonce_id", nullable = false)
    private Annonce annonce;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
