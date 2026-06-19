package sn.immosn.backend.shared.service;

/**
 * Envoie au visiteur (prospect non authentifié) son numéro de suivi après une demande de visite,
 * par SMS (Twilio) et par email. Les deux canaux sont indépendants : l'échec de l'un n'empêche
 * pas l'envoi de l'autre. Désactivés individuellement si leurs credentials ne sont pas configurés
 * (twilio.enabled / mail.enabled).
 */
public interface VisiteTrackingNotificationService {

    void notifierNumeroSuivi(String nom, String email, String telephone, String token);
}
