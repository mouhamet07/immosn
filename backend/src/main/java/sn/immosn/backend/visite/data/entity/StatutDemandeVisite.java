package sn.immosn.backend.visite.data.entity;

public enum StatutDemandeVisite {
    EN_ATTENTE,
    ACCEPTEE,
    /** Visite acceptée et affectée à un administrateur responsable (Sprint 2). */
    AFFECTEE,
    /** Une replanification de la date a été demandée — en attente d'accord (Sprint 2). */
    REPLANIFICATION_DEMANDEE,
    /** L'administrateur responsable a rédigé le rapport de visite (Sprint 2). */
    RAPPORT_REDIGE,
    REFUSEE,
    ANNULEE,
    /** @deprecated Utiliser CLOTUREE_SANS_SUITE ou CLOTUREE_AVEC_CONTRAT. Conservé pour la compatibilité des données historiques. */
    @Deprecated
    TERMINEE,
    /** Visite effectuée — client non intéressé, aucun contrat créé. */
    CLOTUREE_SANS_SUITE,
    /** Visite effectuée — contrat créé automatiquement par l'administrateur. */
    CLOTUREE_AVEC_CONTRAT
}
