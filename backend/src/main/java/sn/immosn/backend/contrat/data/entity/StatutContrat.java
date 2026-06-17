package sn.immosn.backend.contrat.data.entity;

public enum StatutContrat {
    /** Contrat créé, en attente de validation admin */
    EN_ATTENTE,

    /** Pré-contrat brouillon, pas encore soumis au client (Sprint 3) */
    BROUILLON,

    /** Pré-contrat en attente de validation par le client (Sprint 3) */
    EN_ATTENTE_VALIDATION_CLIENT,

    /** Validé par le client, en attente d'activation par le SUPER_ADMIN (Sprint 3) */
    EN_ATTENTE_VALIDATION_SUPER_ADMIN,

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
