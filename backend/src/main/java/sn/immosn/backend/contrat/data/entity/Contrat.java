package sn.immosn.backend.contrat.data.entity;

import jakarta.persistence.*;
import lombok.*;
import sn.immosn.backend.annonce.data.entity.Annonce;
import sn.immosn.backend.auth.data.entity.User;
import sn.immosn.backend.lead.data.entity.Lead;

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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private User client;

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

    /** Horodatage de validation du pré-contrat par le client (Sprint 3). Null tant que non validé. */
    @Column(name = "valide_par_client_at")
    private LocalDateTime valideParClientAt;

    /** Horodatage d'activation du contrat par le SUPER_ADMIN (Sprint 3). Null tant que non activé. */
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
