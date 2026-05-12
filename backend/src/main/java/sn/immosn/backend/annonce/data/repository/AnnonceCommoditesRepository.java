package sn.immosn.backend.annonce.data.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import sn.immosn.backend.annonce.data.entity.AnnonceCommodite;
import sn.immosn.backend.annonce.data.entity.AnnonceCommoditeId;

public interface AnnonceCommoditesRepository
extends JpaRepository<AnnonceCommodite, AnnonceCommoditeId> {
    void deleteByAnnonceId(Long annonceId);
    List<AnnonceCommodite> findByAnnonceId(Long annonceId);
}
