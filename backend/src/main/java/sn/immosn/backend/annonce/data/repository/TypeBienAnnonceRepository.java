package sn.immosn.backend.annonce.data.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import sn.immosn.backend.annonce.data.entity.TypeBienAnnonce;

@Repository
public interface TypeBienAnnonceRepository extends JpaRepository<TypeBienAnnonce, Long> {
    List<TypeBienAnnonce> findByIsArchivedFalse();
    Optional<TypeBienAnnonce> findByIdAndIsArchivedFalse(Long id);
    boolean existsByLibelleIgnoreCase(String libelle);
}
