package sn.immosn.backend.annonce.data.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
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
@Table(name = "commodites")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Commodite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String libelle;

    @Builder.Default                   
    @Column(name = "is_archived", nullable = false)
    private boolean isArchived = false;
    
    @Builder.Default  
    @OneToMany(mappedBy = "commodite",  
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<AnnonceCommodite> annonceCommodites = new ArrayList<>();
}
