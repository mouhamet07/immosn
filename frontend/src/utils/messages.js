// Messages centralisés pour toutes les actions de l'application
export const AUTH_MESSAGES = {
  loginSuccessClient:     'Bienvenue sur ImmoSN ! Vous êtes connecté.',
  loginSuccessAdmin:      "Bienvenue dans l'espace administrateur.",
  loginSuccessSuperAdmin: 'Bienvenue Super Administrateur.',
  invalidCredentials:     'Email ou mot de passe incorrect.',
  accountInactive:        "Votre compte est désactivé. Contactez l'assistance.",
  accountNotFound:        'Aucun compte trouvé avec cet email.',
  logoutSuccess:          'Vous avez été déconnecté avec succès.',
  registerSuccess:        'Compte créé avec succès ! Vous pouvez vous connecter.',
  emailAlreadyExists:     'Un compte existe déjà avec cet email.',
  networkError:           'Impossible de contacter le serveur. Vérifiez votre connexion.',
  serverError:            'Une erreur serveur est survenue. Réessayez plus tard.',
  unauthorized:           'Session expirée. Veuillez vous reconnecter.',
  forbidden:              "Vous n'avez pas les droits pour cette action.",
}

export const MESSAGES = {
  ...AUTH_MESSAGES,

  // Profil
  profilUpdated:   'Votre profil a été mis à jour avec succès.',
  avatarUpdated:   'Photo de profil mise à jour avec succès.',
  passwordUpdated: 'Mot de passe modifié avec succès.',

  // Favoris
  favoriAdded:   'Annonce ajoutée à vos favoris.',
  favoriRemoved: 'Annonce retirée de vos favoris.',

  // Visites
  visiteRequested: 'Demande de visite envoyée avec succès.',
  visiteCancelled: 'Demande de visite annulée.',

  // Annonces
  annonceCreated:  'Annonce publiée avec succès.',
  annonceUpdated:  'Annonce modifiée avec succès.',
  annonceArchived: 'Annonce archivée avec succès.',
  annonceRestored: 'Annonce restaurée avec succès.',

  // Admins
  adminCreated:  'Administrateur créé avec succès.',
  adminUpdated:  'Administrateur modifié avec succès.',
  adminRestored: 'Administrateur restauré avec succès.',
  adminRevoked:  'Accès administrateur révoqué.',
}

export function getErrorMessage(error) {
  const status    = error?.response?.status
  const serverMsg = error?.response?.data?.message

  if (!status)        return MESSAGES.networkError
  if (status === 401) return MESSAGES.unauthorized
  if (status === 403) return MESSAGES.forbidden
  if (status === 404) return MESSAGES.accountNotFound
  if (status === 409) return MESSAGES.emailAlreadyExists
  if (status >= 500)  return MESSAGES.serverError

  return serverMsg || MESSAGES.serverError
}
