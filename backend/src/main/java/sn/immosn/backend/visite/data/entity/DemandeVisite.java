package sn.immosn.backend.visite.data.entity;

import jakarta.persistence.*;
import lombok.*;
import sn.immosn.backend.annonce.data.entity.Annonce;
import sn.immosn.backend.auth.data.entity.User;
import sn.immosn.backend.prospect.data.entity.Prospect;

import java.time.LocalDateTime;

@Entity
@Table(name = "demandes_visite")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemandeVisite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Client authentifié à l'origine de la demande.
     * Nullable depuis l'ouverture du parcours visiteur : une demande peut provenir
     * d'un {@link #prospect} non connecté à la place. Exactement l'un des deux est renseigné.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private User client;

    /** Prospect non authentifié à l'origine de la demande (null si demande d'un client connecté). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prospect_id")
    private Prospect prospect;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "annonce_id", nullable = false)
    private Annonce annonce;

    // Coordonnées saisies pour une demande invité (copie figée, indépendante du Prospect).
    // Nulles pour les demandes d'un client connecté — l'identité provient alors du compte.
    @Column(name = "invite_nom")
    private String nom;

    @Column(name = "invite_prenom")
    private String prenom;

    @Column(name = "invite_telephone")
    private String telephone;

    @Column(name = "invite_email")
    private String email;

    @Column(name = "invite_adresse")
    private String adresse;

    @Column(name = "date_visite", nullable = false)
    private LocalDateTime dateVisite;

    /** Heure souhaitée saisie séparément côté invité (la date complète reste dans {@link #dateVisite}). */
    @Column(name = "heure_visite")
    private String heureVisite;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutDemandeVisite statut = StatutDemandeVisite.EN_ATTENTE;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    @Builder.Default
    @Column(name = "is_archived", nullable = false)
    private boolean isArchived = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
