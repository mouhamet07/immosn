package sn.immosn.backend.visite.data.entity;

import jakarta.persistence.*;
import lombok.*;
import sn.immosn.backend.annonce.data.entity.Annonce;
import sn.immosn.backend.auth.data.entity.User;

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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private User client;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "annonce_id", nullable = false)
    private Annonce annonce;

    @Column(name = "date_visite", nullable = false)
    private LocalDateTime dateVisite;

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
