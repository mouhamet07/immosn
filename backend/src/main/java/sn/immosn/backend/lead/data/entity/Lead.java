package sn.immosn.backend.lead.data.entity;

import jakarta.persistence.*;
import lombok.*;
import sn.immosn.backend.annonce.data.entity.Annonce;
import sn.immosn.backend.auth.data.entity.User;
import sn.immosn.backend.prospect.data.entity.Prospect;
import sn.immosn.backend.visite.data.entity.DemandeVisite;

import java.time.LocalDateTime;

@Entity
@Table(name = "leads")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Client authentifié à l'origine du lead. Nullable depuis l'ouverture du parcours visiteur :
     * un lead peut provenir d'un {@link #prospect} non connecté à la place. Exactement l'un des
     * deux est renseigné.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private User client;

    /** Prospect non authentifié à l'origine du lead (null si lead d'un client connecté). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prospect_id")
    private Prospect prospect;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "annonce_id", nullable = false)
    private Annonce annonce;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visite_id")
    private DemandeVisite visite;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutLead statut = StatutLead.EN_COURS;

    @Column(name = "note_admin", columnDefinition = "TEXT")
    private String noteAdmin;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "converted_at")
    private LocalDateTime convertedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
