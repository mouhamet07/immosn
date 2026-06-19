package sn.immosn.backend.contrat.data.entity;

import jakarta.persistence.*;
import lombok.*;
import sn.immosn.backend.auth.data.entity.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "contrat_documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContratDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contrat_id", nullable = false)
    private Contrat contrat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TypeDocumentContrat type;

    @Column(nullable = false, length = 1000)
    private String url;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
