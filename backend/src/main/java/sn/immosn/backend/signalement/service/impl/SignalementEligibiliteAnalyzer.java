package sn.immosn.backend.signalement.service.impl;

import org.springframework.stereotype.Component;
import sn.immosn.backend.contrat.data.entity.Contrat;
import sn.immosn.backend.contrat.data.entity.TypeContrat;
import sn.immosn.backend.signalement.data.entity.StatutSignalement;
import sn.immosn.backend.signalement.service.SignalementEligibilite;

import java.time.LocalDate;
import java.util.Locale;

/**
 * Applique les règles métier d'éligibilité d'un signalement selon le type de contrat (Sprint 4).
 *
 * <ul>
 *   <li><b>VENTE</b> : éligible uniquement si la date du signalement est ≤ date de fin de garantie.
 *       Au-delà (ou sans garantie définie) → NON_ELIGIBLE.</li>
 *   <li><b>LOCATION</b> : toujours éligible (→ EN_ANALYSE) ; on catégorise le signalement
 *       (entretien / réparation / dégradation / responsabilité) à partir du contenu et des clauses,
 *       l'admin tranchant ensuite ACCEPTE / REJETE.</li>
 * </ul>
 */
@Component
public class SignalementEligibiliteAnalyzer {

    public SignalementEligibilite analyser(Contrat contrat, String contenu, LocalDate dateSignalement) {
        TypeContrat type = contrat.getTypeContrat();

        if (type == TypeContrat.VENTE) {
            return analyserVente(contrat, dateSignalement);
        }
        if (type == TypeContrat.LOCATION) {
            return analyserLocation(contenu);
        }
        // Type non renseigné (contrats historiques) : on n'oppose pas de règle, mise en analyse.
        return new SignalementEligibilite(StatutSignalement.EN_ANALYSE,
            "Type de contrat non renseigné — analyse manuelle requise.", "autre");
    }

    private SignalementEligibilite analyserVente(Contrat contrat, LocalDate dateSignalement) {
        LocalDate finGarantie = contrat.getDateFinGarantie();
        if (finGarantie == null) {
            return new SignalementEligibilite(StatutSignalement.NON_ELIGIBLE,
                "Aucune garantie n'est définie sur ce contrat de vente : signalement hors couverture.", null);
        }
        if (dateSignalement.isAfter(finGarantie)) {
            return new SignalementEligibilite(StatutSignalement.NON_ELIGIBLE,
                "Signalement déposé le " + dateSignalement + " après la fin de garantie (" + finGarantie + ").", null);
        }
        return new SignalementEligibilite(StatutSignalement.EN_ANALYSE,
            "Signalement reçu pendant la période de garantie (jusqu'au " + finGarantie + ").", "garantie");
    }

    private SignalementEligibilite analyserLocation(String contenu) {
        String categorie = categoriser(contenu);
        return new SignalementEligibilite(StatutSignalement.EN_ANALYSE,
            "Signalement de location classé : " + categorie + ". Analyse selon les clauses du bail.", categorie);
    }

    /** Catégorise un signalement de location à partir de mots-clés. */
    private String categoriser(String contenu) {
        String c = contenu == null ? "" : contenu.toLowerCase(Locale.FRENCH);
        if (c.contains("dégrad") || c.contains("degrad") || c.contains("cass") || c.contains("abîm") || c.contains("abim")) {
            return "dégradation";
        }
        if (c.contains("répar") || c.contains("repar") || c.contains("panne") || c.contains("fuite") || c.contains("plomb")) {
            return "réparation";
        }
        if (c.contains("entretien") || c.contains("maintenance") || c.contains("nettoy")) {
            return "entretien";
        }
        if (c.contains("responsab") || c.contains("faute") || c.contains("litige")) {
            return "responsabilité";
        }
        return "autre";
    }
}
