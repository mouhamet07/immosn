package sn.immosn.backend.contrat.data.entity;

public enum StatutContrat {
    /** Pré-contrat créé, en attente du document signé puis d'activation par le SUPER_ADMIN */
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
