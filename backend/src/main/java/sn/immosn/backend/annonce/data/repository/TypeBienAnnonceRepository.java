package sn.immosn.backend.annonce.data.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import sn.immosn.backend.annonce.data.entity.TypeBienAnnonce;


public interface TypeBienAnnonceRepository extends JpaRepository<TypeBienAnnonce, Long> {
    List<TypeBienAnnonce> findByIsArchivedFalse();
    boolean existsByLibelleIgnoreCase(String libelle);
}
