package sn.immosn.backend.signalement.data.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Historique des décisions sur un signalement (Sprint 4).
 * Conserve chaque transition de statut, l'action, l'auteur et le motif (notamment de rejet).
 */
@Entity
@Table(name = "signalement_history", indexes = {
    @Index(name = "idx_sh_signalement_id", columnList = "signalement_id"),
    @Index(name = "idx_sh_created_at",     columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignalementHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "signalement_id", nullable = false)
    private Signalement signalement;

    @Enumerated(EnumType.STRING)
    @Column(name = "ancien_statut", length = 50)
    private StatutSignalement ancienStatut;

    @Enumerated(EnumType.STRING)
    @Column(name = "nouveau_statut", length = 50)
    private StatutSignalement nouveauStatut;

    @Column(name = "auteur_email", length = 255)
    private String auteurEmail;

    @Column(name = "action", length = 100, nullable = false)
    private String action;

    @Column(columnDefinition = "TEXT")
    private String motif;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
