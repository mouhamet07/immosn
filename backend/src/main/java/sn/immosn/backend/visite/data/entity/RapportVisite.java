package sn.immosn.backend.visite.data.entity;

import jakarta.persistence.*;
import lombok.*;
import sn.immosn.backend.auth.data.entity.User;

import java.time.LocalDateTime;

/**
 * Rapport rédigé par l'administrateur responsable après avoir effectué une visite (Sprint 2).
 *
 * <p>Un rapport est rattaché à une seule {@link DemandeVisite}. Il consigne le compte-rendu,
 * éventuellement un document téléversé (PDF/image), et l'issue ({@code aboutie}) : visite
 * aboutie (le client souhaite poursuivre) ou non.</p>
 */
@Entity
@Table(name = "rapports_visite")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RapportVisite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "demande_visite_id", nullable = false, unique = true)
    private DemandeVisite demandeVisite;

    /** Administrateur ayant rédigé le rapport. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auteur_admin_id", nullable = false)
    private User auteurAdmin;

    @Column(name = "compte_rendu", columnDefinition = "TEXT")
    private String compteRendu;

    @Column(name = "document_url")
    private String documentUrl;

    /** Issue de la visite : true si le client souhaite poursuivre (vers contrat), false sinon. */
    @Builder.Default
    @Column(nullable = false)
    private boolean aboutie = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
