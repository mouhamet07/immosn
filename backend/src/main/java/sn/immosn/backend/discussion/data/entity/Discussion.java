package sn.immosn.backend.discussion.data.entity;

import jakarta.persistence.*;
import lombok.*;
import sn.immosn.backend.annonce.data.entity.Annonce;
import sn.immosn.backend.auth.data.entity.User;
import sn.immosn.backend.prospect.data.entity.Prospect;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "discussions",
    // Unicité conservée pour les clients connectés : un client = une discussion par annonce.
    // PostgreSQL considère les NULL comme distincts, donc les discussions invité (client_id NULL)
    // ne sont pas contraintes par cette clé — plusieurs visiteurs peuvent discuter de la même annonce.
    uniqueConstraints = @UniqueConstraint(
        name = "uk_discussion_client_annonce",
        columnNames = {"client_id", "annonce_id"}
    )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Discussion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Client authentifié propriétaire de la discussion.
     * Nullable depuis l'ouverture du parcours visiteur : une discussion peut appartenir
     * à un {@link #prospect} non connecté. Exactement l'un des deux est renseigné.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private User client;

    /** Prospect non authentifié propriétaire de la discussion (null pour un client connecté). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prospect_id")
    private Prospect prospect;

    /** Token de suivi non devinable pour retrouver une discussion invité sans authentification. */
    @Column(name = "guest_token", unique = true)
    private String guestToken;

    /** Email du visiteur, copie figée pour une discussion invité (null pour un client connecté). */
    @Column(name = "guest_email")
    private String guestEmail;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "annonce_id", nullable = false)
    private Annonce annonce;

    @Builder.Default
    @OneToMany(mappedBy = "discussion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<Message> messages = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
