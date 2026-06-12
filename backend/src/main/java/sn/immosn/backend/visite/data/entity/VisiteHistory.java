package sn.immosn.backend.visite.data.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "visite_history", indexes = {
    @Index(name = "idx_vh_visite_id",  columnList = "visite_id"),
    @Index(name = "idx_vh_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisiteHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visite_id", nullable = false)
    private DemandeVisite visite;

    @Enumerated(EnumType.STRING)
    @Column(name = "ancien_statut", length = 50)
    private StatutDemandeVisite ancienStatut;

    @Enumerated(EnumType.STRING)
    @Column(name = "nouveau_statut", length = 50)
    private StatutDemandeVisite nouveauStatut;

    @Column(name = "auteur_id")
    private Long auteurId;

    @Column(name = "auteur_email", length = 255)
    private String auteurEmail;

    @Column(name = "ancienne_date_visite")
    private LocalDateTime ancienneDateVisite;

    @Column(name = "nouvelle_date_visite")
    private LocalDateTime nouvelleDateVisite;

    @Column(name = "action", length = 100, nullable = false)
    private String action;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
