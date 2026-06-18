package sn.immosn.backend.contrat.data.entity;

import jakarta.persistence.*;
import lombok.*;
import sn.immosn.backend.annonce.data.entity.Annonce;
import sn.immosn.backend.auth.data.entity.User;
import sn.immosn.backend.lead.data.entity.Lead;
import sn.immosn.backend.prospect.data.entity.Prospect;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "contrats")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contrat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Client titulaire du contrat. Nullable : un contrat issu d'une visite invité n'a pas encore
     * de compte client tant qu'il n'est pas activé par le SUPER_ADMIN — voir {@link #prospect}.
     * Le compte est créé à l'activation finale, jamais avant.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private User client;

    /**
     * Prospect non authentifié à l'origine du contrat (null si le contrat provient d'un client
     * déjà connu). Exactement l'un de {@link #client} ou {@link #prospect} est renseigné avant
     * activation ; après activation, {@link #client} est toujours renseigné.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prospect_id")
    private Prospect prospect;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "annonce_id", nullable = false)
    private Annonce annonce;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id")
    private Lead lead;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montant;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutContrat statut = StatutContrat.EN_ATTENTE;

    /** VENTE ou LOCATION — renseigné à la création, null pour les contrats historiques. */
    @Enumerated(EnumType.STRING)
    @Column(name = "type_contrat", length = 20)
    private TypeContrat typeContrat;

    /** Durée en mois — non null uniquement si typeContrat = LOCATION. */
    @Column(name = "duree_location_mois")
    private Integer dureeLocationMois;

    /**
     * Date de fin de garantie (Sprint 4). Pertinent surtout pour une VENTE :
     * un signalement n'est éligible que si déposé avant cette date. Nullable.
     */
    @Column(name = "date_fin_garantie")
    private LocalDate dateFinGarantie;

    /**
     * Clauses contractuelles (Sprint 4) — texte libre listant les engagements
     * (entretien, réparation, dégradation, responsabilité…) utilisé pour l'analyse
     * d'éligibilité des signalements LOCATION. Nullable.
     */
    @Column(name = "clauses_contractuelles", columnDefinition = "TEXT")
    private String clausesContractuelles;

    @Column(name = "document_url")
    private String documentUrl;

    @Column(columnDefinition = "TEXT")
    private String notes;

    /** Motif fourni par le client lors d'une demande de résiliation. Champ dédié, séparé des notes admin. */
    @Column(name = "motif_resiliation", columnDefinition = "TEXT")
    private String motifResiliation;

    /** Motif fourni par le client lors d'une demande de prolongation (inclut la date proposée si fournie). */
    @Column(name = "motif_prolongation", columnDefinition = "TEXT")
    private String motifProlongation;

    /** Horodatage d'activation du contrat (EN_ATTENTE → ACTIF) par le SUPER_ADMIN. Null tant que non activé. */
    @Column(name = "valide_par_super_admin_at")
    private LocalDateTime valideParSuperAdminAt;

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
