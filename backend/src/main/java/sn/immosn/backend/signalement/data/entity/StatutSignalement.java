package sn.immosn.backend.signalement.data.entity;

public enum StatutSignalement {
    OUVERT,
    EN_COURS,
    RESOLU,
    FERME,
    // Sprint 4 — traitement contractuel des signalements
    /** Hors couverture : VENTE après fin de garantie (rejet automatique à la création). */
    NON_ELIGIBLE,
    /** Éligible, en cours d'analyse (LOCATION : entretien / réparation / dégradation / responsabilité). */
    EN_ANALYSE,
    /** Décision finale : signalement accepté / pris en charge. */
    ACCEPTE,
    /** Décision finale : signalement rejeté (motif obligatoire). */
    REJETE
}
