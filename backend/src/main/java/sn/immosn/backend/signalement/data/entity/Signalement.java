package sn.immosn.backend.signalement.data.entity;

import jakarta.persistence.*;
import lombok.*;
import sn.immosn.backend.auth.data.entity.User;
import sn.immosn.backend.contrat.data.entity.Contrat;
import sn.immosn.backend.contrat.data.entity.TypeContrat;

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

    /** Motif du rejet (Sprint 4) — obligatoire quand le statut passe à REJETE / NON_ELIGIBLE. */
    @Column(name = "motif_rejet", columnDefinition = "TEXT")
    private String motifRejet;

    /**
     * Type du contrat figé à la création (Sprint 4) : VENTE ou LOCATION.
     * Sert de trace de la règle d'éligibilité appliquée, indépendamment d'une évolution ultérieure du contrat.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type_contrat_snapshot", length = 20)
    private TypeContrat typeContratSnapshot;

    /** Horodatage de la décision finale (ACCEPTE / REJETE / NON_ELIGIBLE). Nullable. */
    @Column(name = "decision_at")
    private LocalDateTime decisionAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
