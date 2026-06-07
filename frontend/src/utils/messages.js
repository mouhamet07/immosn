export const AUTH_MESSAGES = {
  loginSuccessClient:    'Bienvenue sur ImmoSN ! Vous êtes connecté.',
  loginSuccessAdmin:     "Bienvenue dans l'espace administrateur.",
  loginSuccessSuperAdmin:'Bienvenue Super Administrateur.',

  invalidCredentials: 'Email ou mot de passe incorrect.',
  accountInactive:    'Votre compte est désactivé. Contactez l\'assistance.',
  accountNotFound:    'Aucun compte trouvé avec cet email.',

  logoutSuccess: 'Vous avez été déconnecté avec succès.',

  registerSuccess:    'Compte créé avec succès ! Vous pouvez vous connecter.',
  emailAlreadyExists: 'Un compte existe déjà avec cet email.',

  networkError: 'Impossible de contacter le serveur. Vérifiez votre connexion.',
  serverError:  'Une erreur serveur est survenue. Réessayez plus tard.',
  unauthorized: 'Session expirée. Veuillez vous reconnecter.',
  forbidden:    "Vous n'avez pas les droits pour cette action.",
}

export function getErrorMessage(error) {
  const status    = error?.response?.status
  const serverMsg = error?.response?.data?.message

  if (!status)      return AUTH_MESSAGES.networkError
  if (status === 401) return AUTH_MESSAGES.unauthorized
  if (status === 403) return AUTH_MESSAGES.forbidden
  if (status === 404) return AUTH_MESSAGES.accountNotFound
  if (status === 409) return AUTH_MESSAGES.emailAlreadyExists
  if (status >= 500)  return AUTH_MESSAGES.serverError

  return serverMsg || AUTH_MESSAGES.serverError
}
