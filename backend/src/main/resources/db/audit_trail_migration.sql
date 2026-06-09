-- =====================================================
-- AUDIT TRAIL — ImmoSN
-- Tables d'historique métier : Contrats, Leads, Visites
-- À exécuter sur Neon PostgreSQL avant le redémarrage
-- =====================================================

-- Historique des transitions de statut des contrats
CREATE TABLE IF NOT EXISTS contrat_history (
    id             BIGSERIAL    PRIMARY KEY,
    contrat_id     BIGINT       NOT NULL REFERENCES contrats(id),
    ancien_statut  VARCHAR(50),
    nouveau_statut VARCHAR(50),
    auteur_id      BIGINT,
    auteur_email   VARCHAR(255),
    action         VARCHAR(100) NOT NULL,
    commentaire    TEXT,
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_ch_contrat_id ON contrat_history(contrat_id);
CREATE INDEX IF NOT EXISTS idx_ch_created_at ON contrat_history(created_at);

-- Historique des transitions de statut des leads
CREATE TABLE IF NOT EXISTS lead_history (
    id             BIGSERIAL    PRIMARY KEY,
    lead_id        BIGINT       NOT NULL REFERENCES leads(id),
    ancien_statut  VARCHAR(50),
    nouveau_statut VARCHAR(50),
    auteur_id      BIGINT,
    auteur_email   VARCHAR(255),
    action         VARCHAR(100) NOT NULL,
    commentaire    TEXT,
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_lh_lead_id    ON lead_history(lead_id);
CREATE INDEX IF NOT EXISTS idx_lh_created_at ON lead_history(created_at);

-- Historique des transitions de statut des visites
CREATE TABLE IF NOT EXISTS visite_history (
    id                    BIGSERIAL    PRIMARY KEY,
    visite_id             BIGINT       NOT NULL REFERENCES demandes_visite(id),
    ancien_statut         VARCHAR(50),
    nouveau_statut        VARCHAR(50),
    auteur_id             BIGINT,
    auteur_email          VARCHAR(255),
    ancienne_date_visite  TIMESTAMP,
    nouvelle_date_visite  TIMESTAMP,
    action                VARCHAR(100) NOT NULL,
    commentaire           TEXT,
    created_at            TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_vh_visite_id  ON visite_history(visite_id);
CREATE INDEX IF NOT EXISTS idx_vh_created_at ON visite_history(created_at);
