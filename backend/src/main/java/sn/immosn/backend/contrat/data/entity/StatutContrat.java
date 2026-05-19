package sn.immosn.backend.contrat.data.entity;

public enum StatutContrat {
    /** Contrat créé, en attente de validation admin */
    EN_ATTENTE,

    /** Contrat en cours, valide */
    ACTIF,

    /** Date de fin dépassée — mis automatiquement par le job planifié */
    EXPIRE,

    /** Contrat résilié définitivement par l'admin */
    RESILIE,

    /** Client a demandé une résiliation — en attente de validation admin */
    EN_ATTENTE_RESILIATION,

    /** Client a demandé une prolongation — en attente de validation admin */
    PROLONGATION_EN_ATTENTE
}
