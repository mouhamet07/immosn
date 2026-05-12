package sn.immosn.backend.annonce.data.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import sn.immosn.backend.annonce.data.entity.AnnonceCommodite;
import sn.immosn.backend.annonce.data.entity.AnnonceCommoditeId;

@Repository
public interface AnnonceCommoditesRepository
extends JpaRepository<AnnonceCommodite, AnnonceCommoditeId> {
    void deleteByAnnonceId(Long annonceId);
    List<AnnonceCommodite> findByAnnonceId(Long annonceId);
}
