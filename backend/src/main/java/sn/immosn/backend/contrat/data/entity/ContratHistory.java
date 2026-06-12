package sn.immosn.backend.contrat.data.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "contrat_history", indexes = {
    @Index(name = "idx_ch_contrat_id", columnList = "contrat_id"),
    @Index(name = "idx_ch_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContratHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contrat_id", nullable = false)
    private Contrat contrat;

    @Enumerated(EnumType.STRING)
    @Column(name = "ancien_statut", length = 50)
    private StatutContrat ancienStatut;

    @Enumerated(EnumType.STRING)
    @Column(name = "nouveau_statut", length = 50)
    private StatutContrat nouveauStatut;

    @Column(name = "auteur_id")
    private Long auteurId;

    @Column(name = "auteur_email", length = 255)
    private String auteurEmail;

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
