-- ============================================================================
-- Migration Sprint 7 — Suppression du circuit "pré-contrat" (validation client en ligne)
-- ============================================================================
--
-- Contexte :
-- Le circuit de validation en ligne du contrat par le client (statuts BROUILLON,
-- EN_ATTENTE_VALIDATION_CLIENT, EN_ATTENTE_VALIDATION_SUPER_ADMIN, méthodes
-- validerParClient / activerParSuperAdmin, page PreContratValidationView.vue) ne
-- correspondait à aucun processus métier réel et a été retiré du code applicatif.
--
-- Le workflow est désormais simplifié : EN_ATTENTE → ACTIF, la transition vers
-- ACTIF étant réservée au SUPER_ADMIN via l'endpoint générique de mise à jour
-- du contrat (PUT /contrats/{id}).
--
-- Colonne supprimée : contrats.valide_par_client_at (horodatage de l'ancienne
-- validation client, devenu inutile).
--
-- Colonne CONSERVÉE : contrats.valide_par_super_admin_at — réutilisée comme
-- horodatage de l'activation finale simplifiée (EN_ATTENTE → ACTIF). Ne pas
-- supprimer cette colonne.
--
-- Cette migration est idempotente (IF EXISTS) mais reste destructive sur la
-- colonne visée : sauvegarder si une restauration ultérieure est envisagée.
--
-- ============================================================================
-- Vérification préalable recommandée (à exécuter avant la migration) :
--
-- SELECT id, statut FROM contrats
-- WHERE statut IN ('BROUILLON', 'EN_ATTENTE_VALIDATION_CLIENT', 'EN_ATTENTE_VALIDATION_SUPER_ADMIN');
--
-- Si cette requête retourne des lignes, les basculer manuellement vers EN_ATTENTE
-- avant de déployer le nouveau code (le nouvel enum Java ne reconnaît plus ces
-- valeurs et ddl-auto=validate ne détecte pas ce type d'incohérence applicative) :
--
-- UPDATE contrats SET statut = 'EN_ATTENTE'
-- WHERE statut IN ('BROUILLON', 'EN_ATTENTE_VALIDATION_CLIENT', 'EN_ATTENTE_VALIDATION_SUPER_ADMIN');
-- ============================================================================

BEGIN;

ALTER TABLE contrats DROP COLUMN IF EXISTS valide_par_client_at;

COMMIT;

-- ============================================================================
-- Récapitulatif :
--   - 1 colonne supprimée : contrats.valide_par_client_at
--   - contrats.valide_par_super_admin_at conservée et réutilisée pour
--     l'horodatage de l'activation simplifiée (EN_ATTENTE → ACTIF)
--   - L'application peut ensuite démarrer avec spring.jpa.hibernate.ddl-auto=validate
--
-- Note environnement de développement (H2) :
--   Aucune action requise — ddl-auto=update régénère automatiquement le schéma
--   au prochain démarrage local et ajuste la colonne selon le mapping JPA mis à jour.
-- ============================================================================
