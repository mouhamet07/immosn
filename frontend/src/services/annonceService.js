import api from './api'

/**
 * Valide qu'un ID est un entier positif avant de l'injecter dans une URL.
 * Prévient les attaques SSRF par manipulation de l'URL.
 */
function validateId(id) {
  const parsed = Number(id)
  if (!Number.isInteger(parsed) || parsed <= 0) {
    throw new Error(`ID invalide : "${id}"`)
  }
  return parsed
}

export default {
  // Récupérer toutes les annonces avec filtres optionnels
  getAllAnnonces(filters = {}) {
    return api.get('/annonces', { params: filters })
  },

  // Récupérer le détail d'une annonce par ID
  getAnnonceById(id) {
    return api.get(`/annonces/${validateId(id)}`)
  },

  // Créer une annonce (ADMIN uniquement)
  createAnnonce(data) {
    return api.post('/annonces', data)
  },

  // Modifier une annonce (ADMIN uniquement)
  updateAnnonce(id, data) {
    return api.put(`/annonces/${validateId(id)}`, data)
  },

  // Archiver une annonce (ADMIN uniquement)
  archiveAnnonce(id) {
    return api.delete(`/annonces/${validateId(id)}`)
  },
}
