package sn.immosn.backend.annonce.data.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "annonces")
@Data
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class Annonce {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String libelle;
    @Column(columnDefinition = "TEXT", nullable = true)
    private String description;
    @Column(nullable = false)
    private Integer nbrPieces;
    @Column(nullable = false)
    private Double surface;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal prix;
    @Column(nullable = false)
    private String adresse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_bien_id", nullable = false)
    private TypeBienAnnonce typeBien;

    @Builder.Default
    @OneToMany(mappedBy = "annonce",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<AnnonceCommodite> annonceCommodites = new ArrayList<>();

    @Builder.Default
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "annonce_images",
        joinColumns = @JoinColumn(name = "annonce_id")
    )
    @Column(name = "image_url")
    private List<String> images = new ArrayList<>();

    @Builder.Default
    @Column(name = "is_archived", nullable = false)
    private boolean isArchived = false;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
