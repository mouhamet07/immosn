package sn.immosn.backend.signalement.data.entity;

import jakarta.persistence.*;
import lombok.*;
import sn.immosn.backend.auth.data.entity.User;
import sn.immosn.backend.contrat.data.entity.Contrat;

import java.time.LocalDateTime;

@Entity
@Table(name = "signalements")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Signalement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contrat_id", nullable = false)
    private Contrat contrat;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private User client;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenu;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutSignalement statut = StatutSignalement.OUVERT;

    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(name = "reponse_admin", columnDefinition = "TEXT")
    private String reponseAdmin;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
