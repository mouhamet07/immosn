package sn.immosn.backend.annonce.data.repository;

import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import sn.immosn.backend.annonce.data.entity.Annonce;
import sn.immosn.backend.annonce.data.entity.AnnonceCommodite;
import sn.immosn.backend.client.web.annonce.dto.SearchAnnonceRequestDto;

import java.util.ArrayList;
import java.util.List;

public class AnnonceSpecification {

    private AnnonceSpecification() {}

    public static Specification<Annonce> fromRequest(SearchAnnonceRequestDto req) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Exclure les annonces archivées
            predicates.add(cb.isFalse(root.get("isArchived")));

            if (req.typeBienId() != null) {
                predicates.add(cb.equal(root.get("typeBien").get("id"), req.typeBienId()));
            }

            if (req.prixMin() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("prix"), req.prixMin()));
            }

            if (req.prixMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("prix"), req.prixMax()));
            }

            if (req.adresse() != null && !req.adresse().isBlank()) {
                predicates.add(cb.like(
                    cb.lower(root.get("adresse")),
                    "%" + req.adresse().toLowerCase() + "%"
                ));
            }

            if (req.nbrPieces() != null) {
                predicates.add(cb.equal(root.get("nbrPieces"), req.nbrPieces()));
            }

            if (req.piecesMin() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("nbrPieces"), req.piecesMin()));
            }

            if (req.piecesMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("nbrPieces"), req.piecesMax()));
            }

            // Filtre commodités : l'annonce doit posséder TOUTES les commodités demandées
            // AnnonceCommodite utilise @IdClass avec champs annonce/commodite (pas d'embedded id)
            if (req.commoditeIds() != null && !req.commoditeIds().isEmpty()) {
                for (Long commoditeId : req.commoditeIds()) {
                    Subquery<Long> sub = query.subquery(Long.class);
                    Root<AnnonceCommodite> acRoot = sub.from(AnnonceCommodite.class);
                    sub.select(acRoot.get("annonce").get("id"))
                        .where(
                            cb.equal(acRoot.get("annonce").get("id"), root.get("id")),
                            cb.equal(acRoot.get("commodite").get("id"), commoditeId)
                        );
                    predicates.add(cb.exists(sub));
                }
            }

            // Éviter les doublons causés par les jointures
            if (query.getResultType() != Long.class) {
                query.distinct(true);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
