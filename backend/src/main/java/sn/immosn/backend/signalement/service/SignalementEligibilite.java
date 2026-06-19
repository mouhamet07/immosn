package sn.immosn.backend.signalement.service;

import sn.immosn.backend.signalement.data.entity.StatutSignalement;

/**
 * Résultat de l'analyse d'éligibilité d'un signalement à sa création (Sprint 4).
 *
 * @param statutInitial statut attribué : EN_ANALYSE (éligible) ou NON_ELIGIBLE (hors couverture)
 * @param motif         explication (motif de non-éligibilité, ou catégorie d'analyse LOCATION)
 * @param categorie     pour une LOCATION : entretien / réparation / dégradation / responsabilité / autre
 */
public record SignalementEligibilite(
    StatutSignalement statutInitial,
    String motif,
    String categorie
) {
    public boolean estEligible() {
        return statutInitial != StatutSignalement.NON_ELIGIBLE;
    }
}
