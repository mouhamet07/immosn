package sn.immosn.backend.visite.data.entity;

public enum StatutDemandeVisite {
    EN_ATTENTE,
    ACCEPTEE,
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
