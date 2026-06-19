package sn.immosn.backend.prospect.data.entity;

import jakarta.persistence.*;
import lombok.*;
import sn.immosn.backend.auth.data.entity.User;
import sn.immosn.backend.discussion.data.entity.Discussion;
import sn.immosn.backend.visite.data.entity.DemandeVisite;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Visiteur non authentifié ayant initié un contact ou une demande de visite.
 *
 * <p>Découple le parcours commercial du compte {@code User} : un prospect peut contacter
 * l'agence et demander des visites sans s'inscrire. Le {@code token} (UUID) sert de référence
 * de suivi non devinable, transmise au visiteur pour retrouver sa conversation.</p>
 *
 * <p>Lors d'une conversion ultérieure (hors périmètre Sprint 1), {@code convertedUser} pointe
 * vers le compte CLIENT généré ; le prospect reste conservé pour l'historique.</p>
 */
@Entity
@Table(name = "prospects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prospect {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    private String prenom;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String telephone;

    private String adresse;

    /** Référence de suivi non devinable (UUID) communiquée au visiteur. */
    @Column(nullable = false, unique = true, updatable = false)
    private String token;

    /** Compte CLIENT issu d'une conversion ultérieure — null tant que le prospect n'est pas converti. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "converted_user_id")
    private User convertedUser;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder.Default
    @OneToMany(mappedBy = "prospect")
    private List<DemandeVisite> demandesVisite = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "prospect")
    private List<Discussion> discussions = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
