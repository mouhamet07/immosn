package sn.immosn.backend.annonce.data.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "type_bien_annonces")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TypeBienAnnonce {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String libelle;

    @Builder.Default                    
    @Column(name = "is_archived", nullable = false)
    private Boolean isArchived = false;

    @OneToMany(mappedBy = "typeBien")
    private List<Annonce> annonces = new ArrayList<>();
}
